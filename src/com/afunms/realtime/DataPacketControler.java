package com.afunms.realtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.afunms.common.util.SysLogger;
import com.afunms.indicators.dao.NodeGatherIndicatorsDao;
import com.afunms.indicators.model.NodeDTO;
import com.afunms.indicators.model.NodeGatherIndicators;
import com.afunms.indicators.util.NodeUtil;
import com.afunms.monitor.executor.base.SnmpMonitor;
import com.afunms.polling.PollingEngine;
import com.afunms.polling.node.Host;
import com.afunms.polling.om.InPkts;
import com.afunms.polling.om.OutPkts;
import com.afunms.polling.om.UtilHdx;
import com.afunms.polling.snmp.interfaces.PackageSnmp;
/********************************************************
 *Title:�㲥 amcharts ���������ļ�
 *Description:
 *Company  dhcc
 *@author songrb
 * Oct 11, 2011 14:02:56 FM
 ********************************************************
 */
public class DataPacketControler extends SnmpMonitor{
	private Logger logger=Logger.getLogger(PortControler.class);
	private DoubleDataQueue doubleDataQueue=null;
	public static void main(String args[]) {
	}
	/**
	 * ��������
	 * @param fileName �ļ���
	 * @param blackFlag �Ƿ���Ҫ���ɳ�ʼ������
	 * @param nodeID
	 * @param ifindex
	 * @param rq
	 * @param cx
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public String generateData(String fileName,boolean blackFlag,int nodeID,int ifindex,HttpServletRequest rq,ServletContext cx){
		String webAppPath=cx.getRealPath("/");
		String path=webAppPath+"amcharts_data/"+fileName;
		//SysLogger.info("The amcharts data path is:"+path);
		HttpSession session=rq.getSession();
		this.doubleDataQueue=(DoubleDataQueue)session.getAttribute("portqueue");
		if(null==this.doubleDataQueue){
			this.doubleDataQueue=new DoubleDataQueue();
		}
		if(true==blackFlag){//���ɿ��ļ� ǰn-1������Ϊnull ���һ������Ϊ0.0
			this.doubleDataQueue.initWithLastData(0.0);//���һ������Ϊ0
			this.doubleDataQueue.setDataList(false);//������ʵ����
			session.setAttribute("portqueue", this.doubleDataQueue);
		}else{
			DoubleDataModel doubleDM=null;
			doubleDM=getPortData(nodeID,ifindex);//       �ɼ�����
			if(null==doubleDM){//�ɼ�����ʧ�� 
				return "failed:�ɼ�����ʧ��";
			}else{
				if(false==this.doubleDataQueue.isDataList()){
					this.doubleDataQueue.getList().removeLast();//����ʼ����0ֵȥ��
					this.doubleDataQueue.setDataList(true);
				}
				//SysLogger.info("The Data is:"+doubleDM.getFirstData()+"______"+doubleDM.getSecondData()+"__"+doubleDM.getDate());
				this.doubleDataQueue.enqueue(doubleDM);
				session.setAttribute("portqueue", this.doubleDataQueue);
			}
		}
		int size=this.doubleDataQueue.getLENGTH();
		SimpleDateFormat smft=new SimpleDateFormat("ss");//�������ڸ�ʽ
		String date;
		String data;
		StringBuffer dataXML=new StringBuffer("");
		dataXML.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		dataXML.append("<chart><series>");
		for(int i=0;i<size;i++){
			date=smft.format(this.doubleDataQueue.getList().get(i).getDate());
			// <value xid="0">1850</value>
			dataXML.append("<value xid=\"").append(i).append("\">").append(date).append("</value>");
		}

		dataXML.append("</series><graphs><graph gid=\"1\">");
		for(int i=0;i<size;i++){
			data=this.doubleDataQueue.getList().get(i).getFirstData()+"";
			//  <value xid="0">-0.447</value>
				dataXML.append("<value xid=\"").append(i).append("\">").append(data).append("</value>");
		}
		dataXML.append("</graph><graph gid=\"2\">");
		for(int i=0;i<size;i++){
			data=this.doubleDataQueue.getList().get(i).getSecondData()+"";
			//  <value xid="0">-0.447</value>
				dataXML.append("<value xid=\"").append(i).append("\">").append(data).append("</value>");
		}
		dataXML.append("</graph></graphs></chart>");
		write(path, dataXML.toString());
		return "success";
	}
	/**
	 * д�ļ�
	 * @param path
	 * @param content
	 * void
	 */
	public  void write(String path, String content) {
		try {
			File f = new File(path);
			if (f.exists()) {
				f.delete();
				f.createNewFile();
			} else {
				if (f.createNewFile()) {
				} else {
					logger.error("�ļ�����ʧ�ܣ�");
				}
			}
			BufferedWriter output = new BufferedWriter(new FileWriter(f));
			output.write(content);
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * �ɼ�����
	 * @param nodeID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public DoubleDataModel getPortData(int nodeID,int ifindex){
		DoubleDataModel doubleDataModel=new DoubleDataModel();
		Host hostNode = (Host)PollingEngine.getInstance().getNodeByID(nodeID);
		if(null==hostNode){
			SysLogger.info("�ýڵ㲻���ڣ����ܲɼ����ݣ�");
			return null;
		}else if(!hostNode.isManaged()){
			SysLogger.info("�ýڵ㱻���������ܲɼ����ݣ�");//��������ʲô��˼���������������˴��ο�WindowsCpuSnmp.java
			return null;
		}
		List<NodeGatherIndicators> gatherlist = new ArrayList<NodeGatherIndicators>();
		NodeDTO nodeDTO = null;// * ����Ϊ��׼���ܼ��ָ��
		NodeUtil nodeutil = new NodeUtil();//����Ϊ ͨ��  ���� �� �������� ��ȡ �豸
		nodeDTO = nodeutil.creatNodeDTOByNode(hostNode);
		NodeGatherIndicatorsDao indicatorsdao = new NodeGatherIndicatorsDao();//�豸���ܼ��ָ�� dao
		try {
			gatherlist = indicatorsdao.getByNodeidAndType(hostNode.getId()+"", 1, nodeDTO.getType());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			indicatorsdao.close();
		}
		
		if (gatherlist != null && gatherlist.size() > 0) {
			for (int i = 0; i < gatherlist.size(); i++) {
				NodeGatherIndicators nodeGatherIndicators = (NodeGatherIndicators) gatherlist.get(i);
				if ("interface".equalsIgnoreCase(nodeGatherIndicators.getName())) {
					// ���нӿ���Ϣ�Ĳɼ�
					PackageSnmp	packageSnmp=null;
					try {
						packageSnmp = (PackageSnmp) Class.forName(
						"com.afunms.polling.snmp.interfaces.PackageSnmp").newInstance();
						Hashtable returnHash = packageSnmp.collect_Data(nodeGatherIndicators);
						Vector  inpacksVector = (Vector) returnHash.get("inpacks");						
						if (inpacksVector != null && inpacksVector.size() > 0) {
							for(int ii=0;ii<inpacksVector.size();ii++){
								InPkts inpkts=(InPkts)inpacksVector.get(ii);
								if(ifindex==Integer.valueOf(inpkts.getSubentity())){
									if("ifInBroadcastPkts".equalsIgnoreCase(inpkts.getEntity())){
										doubleDataModel.setFirstData(Double.valueOf(inpkts.getThevalue()));
										doubleDataModel.setDate(inpkts.getCollecttime().getTime());
									}
								}
							}
						}else{
							Date date=Calendar.getInstance().getTime();
							doubleDataModel.setFirstData(0.0);
							doubleDataModel.setDate(date);
						}
						
						Vector outpacksVector = (Vector) returnHash.get("outpacks");
						if (outpacksVector != null && outpacksVector.size() > 0) {
							for(int ii=0;ii<inpacksVector.size();ii++){
								OutPkts outpkts=(OutPkts)outpacksVector.get(ii);
								if(ifindex==Integer.valueOf(outpkts.getSubentity())){
									if("ifOutBroadcastPkts".equalsIgnoreCase(outpkts.getEntity())){
										doubleDataModel.setSecondData(Double.valueOf(outpkts.getThevalue()));
										doubleDataModel.setDate(outpkts.getCollecttime().getTime());
									}
								}
							}
						}else{
							Date date=Calendar.getInstance().getTime();
							doubleDataModel.setSecondData(0.0);
							doubleDataModel.setDate(date);
						}						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return doubleDataModel;
	}
}
package com.afunms.automation.task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.afunms.automation.dao.NetCfgFileNodeDao;
import com.afunms.automation.manage.NetCfgFileManager;
import com.afunms.automation.model.NetCfgFileNode;
import com.afunms.automation.telnet.BaseTelnet;
import com.afunms.automation.telnet.CiscoTelnet;
import com.afunms.automation.telnet.NetTelnetUtil;
import com.afunms.automation.telnet.RedGiantTelnet;
import com.afunms.automation.telnet.ZteTelnet;
import com.afunms.capreport.common.DateTime;
import com.afunms.capreport.dao.BaseDaoImp;
import com.afunms.common.util.SysLogger;
import com.afunms.initialize.ResourceCenter;
import com.afunms.polling.ssh.SSHUtil;
import com.afunms.polling.task.MonitorTask;
import com.database.config.SystemConfig;

/**
 * 定时备份配置文件
 * 
 */
public class TimingBkpTask extends MonitorTask {
	public static Logger log = Logger.getLogger(TimingBkpTask.class);

	private final String hms = " 00:00:00";

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		subscribe();
		// System.gc();
	}

	/**
	 * 定时备份配置文件
	 */
	public void subscribe() {
	
		DateTime dt = new DateTime();
		String time = dt.getMyDateTime(DateTime.Datetime_Format_14);
		
		String sql = "SELECT * FROM sys_timingbackup_telnetconfig s WHERE status = '1' and s.BACKUP_DATE > 10000";
		
		//在配置文件中设置是否启动定制发送报表 wxy add
		String flag= SystemConfig.getConfigInfomation("Agentconfig","Configserver");
		ArrayList<Map<String, String>> ssconfAL=null;
		if(flag!=null&&flag.equals("enable")){
			BaseDaoImp cd = new BaseDaoImp();
			ssconfAL = cd.executeQuery(sql);
		}
		Map<String, String> ssidAL = null;
		if (ssconfAL != null) {
			log.info("-------------------------------(定时备份)定时器执行时间：" + dt.getMyDateTime(DateTime.Datetime_Format_2) + "-------------------------------");
			try {
				for (int i = 0; i < ssconfAL.size(); i++) {
					ssidAL = ssconfAL.get(i);
					String status = ssidAL.get("status");
					String telnetconfigips = ssidAL.get("telnetconfigips");
					String backup_sendfrequency = ssidAL.get("BACKUP_SENDFREQUENCY");
					String backup_time_month = ssidAL.get("BACKUP_TIME_MONTH");
					String backup_time_week = ssidAL.get("BACKUP_TIME_WEEK");
					String backup_time_day = ssidAL.get("BACKUP_TIME_DAY");
					String backup_time_hou = ssidAL.get("BACKUP_TIME_HOU");
					String exportType = ssidAL.get("ATTACHMENTFORMAT");
					String bkpType = ssidAL.get("bkpType");
					String content = ssidAL.get("content");
					String id = ssidAL.get("id");
					boolean istrue = false;
					// 发送频率，0:全部发送;1:每天;2:每周;3:每月;4每季度;5每年
					if ("0".equals(backup_sendfrequency)) {
						istrue = true;
					} else if ("1".equals(backup_sendfrequency)) {
						if (backup_time_hou.contains("/" + (dt.getHours() < 10 ? "0" + dt.getHours() : dt.getHours()) + "/")) {
							istrue = true;
						}
					} else if ("2".equals(backup_sendfrequency)) {
						if (backup_time_week.contains("/" + (dt.getDay() - 1) + "/") && backup_time_hou.contains("/" + (dt.getHours() < 10 ? "0" + dt.getHours() : dt.getHours()) + "/")) {
							istrue = true;
						}
					} else if ("3".equals(backup_sendfrequency)) {
						if (backup_time_day.contains("/" + (dt.getDate() < 10 ? "0" + dt.getDate() : dt.getDate()) + "/")
								&& backup_time_hou.contains("/" + (dt.getHours() < 10 ? "0" + dt.getHours() : dt.getHours()) + "/")) {
							istrue = true;
						}
					} else if ("4".equals(backup_sendfrequency)) {
						if (backup_time_month.contains("/" + (dt.getMonth() < 10 ? "0" + dt.getMonth() : dt.getMonth()) + "/")
								&& backup_time_day.contains("/" + (dt.getDate() < 10 ? "0" + dt.getDate() : dt.getDate()) + "/")
								&& backup_time_hou.contains("/" + (dt.getHours() < 10 ? "0" + dt.getHours() : dt.getHours()) + "/")) {
							istrue = true;
						}
					} else if ("5".equals(backup_sendfrequency)) {
						if (backup_time_month.contains("/" + (dt.getMonth() < 10 ? "0" + dt.getMonth() : dt.getMonth()) + "/")
								&& backup_time_day.contains("/" + (dt.getDate() < 10 ? "0" + dt.getDate() : dt.getDate()) + "/")
								&& backup_time_hou.contains("/" + (dt.getHours() < 10 ? "0" + dt.getHours() : dt.getHours()) + "/")) {
							istrue = true;
						}
					}
					if (istrue) {
						log.info("定时备份配置文件开始--telnetconfigips=" + telnetconfigips);
						// String filePath =
						// ResourceCenter.getInstance().getSysPath() + "temp" +
						// File.separator
						// + "backup" + "_" + System.currentTimeMillis() + "." +
						// exportType;
						// if (doSubscribe(ssidAL, filePath)) {
						// sendMail(ssidAL, filePath);
						// } else {
						// log.error("订阅" + subscribe_id + "失败！");
						// }
						if (!telnetconfigips.equals("") && telnetconfigips != null) {
							String[] ips = telnetconfigips.split(",");
							NetCfgFileNodeDao dao = new NetCfgFileNodeDao();
							for (String ip : ips) {
								if (ip != null && !ip.equals("") && !ip.equals(",") && "1".equals(status)) {
									NetCfgFileNode vo = (NetCfgFileNode) dao.loadByIp(ip);
									if (vo != null) {
										SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HH-mm");
										String b_time = sdf.format(new Date());
										String prefix = ResourceCenter.getInstance().getSysPath().replace("\\", "/");
										String fileName = prefix + "cfg/" + vo.getIpaddress() + "_" + b_time + "cfg.cfg";
										String descri = vo.getIpaddress() + "_" + b_time;

										if (bkpType.equals("0")) {
											fileName = prefix + "cfg/" + vo.getIpaddress() + "(" + i + ")_" + b_time + "log.log";
											bkpLog(ip, fileName, content, descri, id);
										} else {
											bkpCfg_1(ip, fileName, descri, bkpType);
										}
									}
								}
							}
							dao.close();
						}
					}
				}
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}

	/**
	 * 备份配置文件
	 * 
	 * @param id
	 * @param fileName
	 * @param fileDesc
	 * @param bkpType
	 */
	private void bkpCfg_1(String ip, String fileName, String fileDesc, String bkpType) {
		// String id = getParaValue("id");//NetCfgFileNode 的主键ID
		// String fileName = this.getParaValue("fileName");
		// String fileDesc = this.getParaValue("fileDesc");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HH-mm");
		String bkptime = "";
		Date bkpDate = new Date();
		String reg = "_(.*)cfg.cfg";
		Pattern p = Pattern.compile(reg);
		Matcher m = p.matcher(fileName);
		if (m.find()) {
			bkptime = m.group(1);
		}
		try {
			bkpDate = sdf.parse(bkptime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		NetCfgFileNodeDao dao = new NetCfgFileNodeDao();
		NetCfgFileNode vo = null;
		;
		try {
			vo = (NetCfgFileNode) dao.loadByIp(ip);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dao.close();
		}
		String result = "";
		String runBackFileResult = "";// 备份运行时配置文件的结果字符串
		String startupBackFileResult = "";// 备份启动时配置文件的结果字符串
		String jsp = null;
		if(vo.getConnecttype()==1){
			String[] cmds=new String[2];
			String[] cmds2=new String[2];
			SSHUtil t = new SSHUtil(vo.getIpaddress(),vo.getPort(),vo.getUser(), vo.getPassword());
            if ("run".equals(bkpType)) {
            	if(vo.getDeviceRender().equals("h3c")||vo.getDeviceRender().equals("haiwei")){
        			cmds[0]="display current-configuration";
        			cmds[1]="\r";
    			}else if (vo.getDeviceRender().equals("cisco")||vo.getDeviceRender().equals("redgiant")) {
    				cmds[0]="show run";
        			cmds[1]="\r";
				}
            	 result = t.executeCmds(cmds);
			}else if("startup".equals(bkpType)){
				if(vo.getDeviceRender().equals("h3c")||vo.getDeviceRender().equals("haiwei")){
        			cmds[0]="display saved-configuration";
        			cmds[1]="\r";
    			}else if (vo.getDeviceRender().equals("cisco")||vo.getDeviceRender().equals("redgiant")) {
    				cmds[0]="show startup";
        			cmds[1]="\r";
				}
				 result = t.executeCmds(cmds);
				 if (result != null) {
						int beginIndex = result.indexOf(cmds[2]);
						int cmd2Len = beginIndex + cmds[2].length() + 2;
						if (beginIndex > -1 && (result.length() > cmd2Len))
							result = result.substring(cmd2Len);
					}
			}else {
				if(vo.getDeviceRender().equals("h3c")||vo.getDeviceRender().equals("haiwei")){
        			cmds[0]="display saved-configuration";
        			cmds[1]="\r";
        			cmds2[0]="display current-configuration";
        			cmds2[1]="\r";
    			}else if (vo.getDeviceRender().equals("cisco")||vo.getDeviceRender().equals("redgiant")) {
    				cmds[0]="show startup";
        			cmds[1]="\r";
        			cmds2[0]="show run";
        			cmds2[1]="\r";
				}
				runBackFileResult = t.executeCmds(cmds);
				t.disconnect();
				t = new SSHUtil(vo.getIpaddress(),vo.getPort(),vo.getUser(), vo.getPassword());
				startupBackFileResult=t.executeCmds(cmds2);
				if (runBackFileResult != null) {
					int beginIndex = runBackFileResult.indexOf(cmds[0]);
					int cmd2Len = beginIndex + cmds[0].length() + 2;
					if (beginIndex > -1 && (runBackFileResult.length() > cmd2Len))
						runBackFileResult = runBackFileResult.substring(cmd2Len);

				}
				
				if (startupBackFileResult != null) {
					int beginIndex = startupBackFileResult.indexOf(cmds2[0]);
					int cmd2Len = beginIndex + cmds2[0].length() + 2;
					if (beginIndex > -1 && (startupBackFileResult.length() > cmd2Len))
						startupBackFileResult = startupBackFileResult.substring(cmd2Len);
				}
			}
           
			t.disconnect();
			
		}else{
		if (vo.getDeviceRender().equals("h3c")||vo.getDeviceRender().equals("huawei")) {// h3c
			NetTelnetUtil tvpn = new NetTelnetUtil();
			tvpn.setSuUser(vo.getSuuser());// su
			tvpn.setSuPassword(vo.getSupassword());// su密码
			tvpn.setUser(vo.getUser());// 用户
			tvpn.setPassword(vo.getPassword());// 密码
			tvpn.setIp(vo.getIpaddress());// ip地址
			tvpn.setPort(vo.getPort());
			if ("run".equals(bkpType) || "startup".equals(bkpType)) {
				result = tvpn.backupConfFile(bkpType);
			
			} else {// bkpType 为 all的情况
				NetTelnetUtil secondTvpn = new NetTelnetUtil();
				secondTvpn.setSuUser(vo.getSuuser());// su
				secondTvpn.setSuPassword(vo.getSupassword());// su密码
				secondTvpn.setUser(vo.getUser());// 用户
				secondTvpn.setPassword(vo.getPassword());// 密码
				secondTvpn.setIp(vo.getIpaddress());// ip地址
				secondTvpn.setPort(vo.getPort());
				runBackFileResult = tvpn.backupConfFile("run");
				startupBackFileResult = secondTvpn.backupConfFile("startup");
			}
		} else if (vo.getDeviceRender().equals("cisco")) {// cisco
			CiscoTelnet telnet = new CiscoTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(),vo.getPort(),vo.getSuuser(),vo.getSupassword());
			if (telnet.login()) {
				if ("run".equals(bkpType) || "startup".equals(bkpType)) {
					result = telnet.getCfg( bkpType);
				} else {// bkpType 为 all的情况
					CiscoTelnet secondTelnet = new CiscoTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(),vo.getPort(),vo.getSuuser(),vo.getSupassword());
					runBackFileResult = telnet.getCfg( "run");
					if (secondTelnet.login()) {
						startupBackFileResult = secondTelnet.getCfg("startup");
					}
				}
			}
		}else if (vo.getDeviceRender().equals("zte")) {// 中兴wxy add
			ZteTelnet telnet = new ZteTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(),vo.getPort(),vo.getSuuser(),vo.getSupassword());
			String[] results=null;
			if (telnet.tologin()) {
				 results = telnet.getCfg(bkpType);
				if(results!=null&&results.length==2){
					result=results[0];//当其中一种配置时
					runBackFileResult=results[0];//当配置为all
					startupBackFileResult=results[1];//当其中一种配置为all
				}	
			}
			
		}else if (vo.getDeviceRender().equals("redgiant")) {// 锐捷 wxy add
			RedGiantTelnet telnet = new RedGiantTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(),vo.getPort(),vo.getSuuser(),vo.getSupassword());
			String[] results=null;
			if (telnet.tologin()) {
				 results = telnet.getCfg(bkpType);
				if(results!=null&&results.length==2){
					result=results[0];
					runBackFileResult=results[0];
					startupBackFileResult=results[1];
				}	
			}
			
		}
		}
		if ("run".equals(bkpType) || "startup".equals(bkpType)) {
			result = result.replaceAll("  ---- more ----", "").replaceAll("--More--", "").replaceAll("42d", "").replaceAll("                                          ", "").replaceAll("\\\\[", "");

			NetCfgFileManager.getInstance().writeFileAndToDb(bkpType, fileName, fileDesc, bkpDate, vo, result);
		} else {// bkpType = all
			runBackFileResult = runBackFileResult.replaceAll("  ---- more ----", "").replaceAll("--More--", "").replaceAll("42d", "").replaceAll("                                          ", "").replaceAll("\\\\[", "");
			startupBackFileResult = startupBackFileResult.replaceAll("  ---- more ----", "").replaceAll("--More--", "").replaceAll("42d", "").replaceAll("                                          ", "").replaceAll("\\\\[", "");

			NetCfgFileManager.getInstance().writeFileAndToDb("run", fileName, fileDesc, bkpDate, vo, runBackFileResult);
			String startupFileName = fileName.substring(0, fileName.lastIndexOf(".")).concat("(2)").concat(fileName.substring(fileName.lastIndexOf(".")));
			NetCfgFileManager.getInstance().writeFileAndToDb("startup", startupFileName, fileDesc, bkpDate, vo, startupBackFileResult);
		}
	}

	/**
	 * 
	 * @description 备份执行命令结果日志
	 * @author wangxiangyong
	 * @date Feb 25, 2013 11:16:24 AM
	 * @return void  
	 * @param ip
	 * @param fileName
	 * @param content
	 * @param fileDesc
	 * @param id
	 */
	private void bkpLog(String ip, String fileName, String content, String fileDesc, String id) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HH-mm");
		String bkptime = "";
		Date bkpDate = new Date();
		String reg = "_(.*)log.log";
		Pattern p = Pattern.compile(reg);
		Matcher m = p.matcher(fileName);
		if (m.find()) {
			bkptime = m.group(1);
		}
		try {
			bkpDate = sdf.parse(bkptime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		NetCfgFileNodeDao dao = new NetCfgFileNodeDao();
		NetCfgFileNode vo = null;
		try {
			vo = (NetCfgFileNode) dao.loadByIp(ip);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dao.close();
		}
		String result = "";
		String[] commStr = new String[content.split("\r\n").length];
		commStr = content.split("\r\n");
		if(vo.getConnecttype()==1){
			SSHUtil t =null;
			try {
				 t = new SSHUtil(vo.getIpaddress(),vo.getPort(),vo.getUser(), vo.getPassword());
				 result = t.executeCmds(commStr);
			} catch (Exception e) {
				SysLogger.error("M30BackupTelnetConfig.bkpLog()",e);
			}finally{
				if(t!=null){
					t.disconnect();
				}
			}
		}else{
		if (vo.getDeviceRender().equals("h3c")||vo.getDeviceRender().equals("huawei")) {// h3c
			NetTelnetUtil tvpn = new NetTelnetUtil();
			tvpn.setSuUser(vo.getSuuser());// su
			tvpn.setSuPassword(vo.getSupassword());// su密码
			tvpn.setUser(vo.getUser());// 用户
			tvpn.setPassword(vo.getPassword());// 密码
			tvpn.setIp(vo.getIpaddress());// ip地址
			tvpn.setPort(vo.getPort());
			result = tvpn.getCommantValue(commStr);

		} else if (vo.getDeviceRender().equals("cisco")) {// cisco
			CiscoTelnet telnet = new CiscoTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(),vo.getPort(),vo.getSuuser(),vo.getSupassword());
			if (telnet.login()) {
				result = telnet.getCommantValue(commStr);

			}
		} else if (vo.getDeviceRender().equals("redgiant")) {// 锐捷 wxy add
			BaseTelnet telnet = new RedGiantTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(),vo.getPort(),vo.getSuuser(),vo.getSupassword());
			if (telnet.tologin()) {
				result = telnet.getCommantValue(commStr);

			}
		}else if (vo.getDeviceRender().equals("zte")) {// 中兴wxy add
			BaseTelnet telnet = new ZteTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(),vo.getPort(),vo.getSuuser(),vo.getSupassword());
			if (telnet.tologin()) {
				result = telnet.getCommantValue(commStr);

			}
			
		}
		}
		if(result!=null)
		result = result.replaceAll("  ---- more ----", "").replaceAll("--More--", "").replaceAll("42d", "").replaceAll("                                          ", "").replaceAll("\\\\[", "");

		vo.setId(Integer.parseInt(id));// 暂时存放
		NetCfgFileManager.getInstance().writeFileAndToDb("0", fileName, content, bkpDate, vo, result);

	}

	/**
	 * 日期格式转换
	 * 
	 * @param startTime
	 * @return
	 */
	public String startTime(String startTime) {
		return startTime + hms;
	}

	/**
	 * 日期格式转换
	 * 
	 * @param toTime
	 * @return
	 */
	public String toTime(String toTime) {
		String Millisecond = String.valueOf(DateTime.getMillisecond(toTime, DateTime.Datetime_Format_5) - 1000);
		return DateTime.getDateFromMillisecond(Millisecond, DateTime.Datetime_Format_2);
	}
}

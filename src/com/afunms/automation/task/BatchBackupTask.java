package com.afunms.automation.task;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.afunms.automation.manage.NetCfgFileManager;
import com.afunms.automation.model.NetCfgFileNode;
import com.afunms.automation.telnet.CiscoTelnet;
import com.afunms.automation.telnet.NetTelnetUtil;
import com.afunms.automation.telnet.RedGiantTelnet;
import com.afunms.common.util.SysLogger;
import com.afunms.initialize.ResourceCenter;
import com.afunms.polling.ssh.SSHUtil;

public class BatchBackupTask implements Runnable {
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HH-mm");
	StringBuffer result;
	NetCfgFileNode vo;
	String fileName;
	String fileDesc;
	Date bkpDate;
	String bkpType;

	public BatchBackupTask(StringBuffer result, NetCfgFileNode hmo, String fileName, String fileDesc, Date bkpDate, String bkpType) {
		this.result = result;
		this.vo = hmo;
		// this.fileName = hmo.getIpaddress() + fileName;
		// this.fileName = fileName.replace("IP", hmo.getIpaddress());
		String b_time = sdf.format(new Date());
		String prefix = ResourceCenter.getInstance().getSysPath().replace("\\", "/");
		this.fileName = prefix + "cfg/" + hmo.getIpaddress() + "_" + b_time + "cfg.cfg";
		this.fileDesc = fileDesc;
		this.bkpDate = bkpDate;
		this.bkpType = bkpType;
	}

	public void run() {
		String temp_result = "";
		String runBackFileResult = "";// 备份运行时配置文件的结果字符串
		String startupBackFileResult = "";// 备份启动时配置文件的结果字符串
		boolean isSuccess = true;
		if (vo.getConnecttype() == 0) {
			if (vo.getDeviceRender().equals("h3c")||vo.getDeviceRender().equals("huawei"))// h3c
			{
				NetTelnetUtil tvpn = new NetTelnetUtil();
				tvpn.setSuUser(vo.getSuuser());// su
				tvpn.setSuPassword(vo.getSupassword());// su密码
				tvpn.setUser(vo.getUser());// 用户
				tvpn.setPassword(vo.getPassword());// 密码
				tvpn.setIp(vo.getIpaddress());// ip地址
//				tvpn.setDEFAULT_PROMPT(vo.getDefaultpromtp());// 结束标记符号
				tvpn.setPort(vo.getPort());
				// temp_result = tvpn.Backupconffile(bkpType);
				if ("run".equals(bkpType) || "startup".equals(bkpType)) {
					temp_result = tvpn.backupConfFile(bkpType);
				} else {// bkpType 为 all的情况
					NetTelnetUtil secondTvpn = new NetTelnetUtil();
					secondTvpn.setSuUser(vo.getSuuser());// su
					secondTvpn.setSuPassword(vo.getSupassword());// su密码
					secondTvpn.setUser(vo.getUser());// 用户
					secondTvpn.setPassword(vo.getPassword());// 密码
					secondTvpn.setIp(vo.getIpaddress());// ip地址
//					secondTvpn.setDEFAULT_PROMPT(vo.getDefaultpromtp());// 结束标记符号
					secondTvpn.setPort(vo.getPort());
					runBackFileResult = tvpn.backupConfFile("run");
					startupBackFileResult = secondTvpn.backupConfFile("startup");
				}
				if (temp_result.equals("user or password error")) {
					synchronized (result) {
						result.append("," + vo.getIpaddress());
					}
					isSuccess = false;
				}
			} else if (vo.getDeviceRender().equals("cisco"))// cisco
			{
				CiscoTelnet telnet = new CiscoTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(), vo.getPort(), vo.getSuuser(), vo.getSupassword());

				if (telnet.login()) {
					if ("run".equals(bkpType) || "startup".equals(bkpType)) {
						temp_result = telnet.getCfg(bkpType);
					} else {// bkpType 为 all的情况
						CiscoTelnet secondTelnet = new CiscoTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(), vo.getPort(), vo.getSuuser(), vo.getSupassword());

						runBackFileResult = telnet.getCfg("run");
						if (secondTelnet.login()) {
							startupBackFileResult = secondTelnet.getCfg("startup");
						}
					}
				}
			} else if (vo.getDeviceRender().equals("zte")) {// 中兴wxy add
				RedGiantTelnet telnet = new RedGiantTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(), vo.getPort(), vo.getSuuser(), vo.getSupassword());
				String[] results = null;
				if (telnet.tologin()) {
					results = telnet.getCfg(bkpType);
					if (results != null && results.length == 2) {
						temp_result = results[0];// 当其中一种配置时
						runBackFileResult = results[0];// 当配置为all
						startupBackFileResult = results[1];// 当其中一种配置为all
					}
				}

			} else if (vo.getDeviceRender().equals("redgiant")) {// 锐捷 wxy
				// add
				RedGiantTelnet telnet = new RedGiantTelnet(vo.getIpaddress(), vo.getUser(), vo.getPassword(), vo.getPort(), vo.getSuuser(), vo.getSupassword());
				String[] results = null;
				if (telnet.tologin()) {
					results = telnet.getCfg(bkpType);
					if (results != null && results.length == 2) {
						temp_result = results[0];// 当其中一种配置时
						runBackFileResult = results[0];// 当配置为all
						startupBackFileResult = results[1];// 当其中一种配置为all
					}
				}
			}
		} else if (vo.getConnecttype() == 1) {

			String[] cmds = new String[4];
			String[] cmds2 = new String[4];
			SSHUtil t = null;
			try {
				t = new SSHUtil(vo.getIpaddress(), vo.getPort(), vo.getUser(), vo.getPassword());
				if ("run".equals(bkpType)) {
					if (vo.getDeviceRender().equals("h3c") || vo.getDeviceRender().equals("haiwei")) {
						cmds[0] = vo.getSuuser();
						cmds[1] = vo.getSupassword();
						cmds[2] = "display current-configuration";
						cmds[3] = "\r";
					} else if (vo.getDeviceRender().equals("cisco") || vo.getDeviceRender().equals("redgiant")) {
						cmds[0] = vo.getSuuser();
						cmds[1] = vo.getSupassword();
						cmds[2] = "show run";
						cmds[3] = "\r";
					}
					temp_result = t.executeCmds(cmds);
					if (temp_result != null) {
						int beginIndex = temp_result.indexOf(cmds[2]);
						int cmd2Len = beginIndex + cmds[2].length() + 2;
						if (beginIndex > -1 && (temp_result.length() > cmd2Len))
							temp_result = temp_result.substring(cmd2Len);
					}
				} else if ("startup".equals(bkpType)) {
					if (vo.getDeviceRender().equals("h3c") || vo.getDeviceRender().equals("haiwei")) {
						cmds[0] = vo.getSuuser();
						cmds[1] = vo.getSupassword();
						cmds[2] = "display saved-configuration";
						cmds[3] = "\r";
					} else if (vo.getDeviceRender().equals("cisco") || vo.getDeviceRender().equals("redgiant")) {
						cmds[0] = vo.getSuuser();
						cmds[1] = vo.getSupassword();
						cmds[2] = "show startup";
						cmds[3] = "\r";
					}
					temp_result = t.executeCmds(cmds);
					if (temp_result != null) {
						int beginIndex = temp_result.indexOf(cmds[2]);
						int cmd2Len = beginIndex + cmds[2].length() + 2;
						if (beginIndex > -1 && (temp_result.length() > cmd2Len))
							temp_result = temp_result.substring(cmd2Len);
					}
				} else {
					if (vo.getDeviceRender().equals("h3c") || vo.getDeviceRender().equals("haiwei")) {
						cmds[0] = vo.getSuuser();
						cmds[1] = vo.getSupassword();
						cmds[2] = "display saved-configuration";
						cmds[3] = "\r";
						cmds2[0] = vo.getSuuser();
						cmds2[1] = vo.getSupassword();
						cmds2[2] = "display current-configuration";
						cmds2[3] = "\r";
					} else if (vo.getDeviceRender().equals("cisco") || vo.getDeviceRender().equals("redgiant")) {
						cmds[0] = vo.getSuuser();
						cmds[1] = vo.getSupassword();
						cmds[2] = "show startup";
						cmds[3] = "\r";
						cmds2[0] = vo.getSuuser();
						cmds2[1] = vo.getSupassword();
						cmds2[2] = "show run";
						cmds2[3] = "\r";
					}
					try {
						runBackFileResult = t.executeCmds(cmds);
						if (runBackFileResult != null) {
							int beginIndex = runBackFileResult.indexOf(cmds[2]);
							int cmd2Len = beginIndex + cmds[2].length() + 2;
							if (beginIndex > -1 && (runBackFileResult.length() > cmd2Len))
								runBackFileResult = runBackFileResult.substring(cmd2Len);

						}
					} catch (Exception e) {
						SysLogger.error("SSHUtil executeCmds方法执行失败！", e);
					} finally {
						if (t != null)
							t.disconnect();
					}

					t = new SSHUtil(vo.getIpaddress(), vo.getPort(), vo.getUser(), vo.getPassword());
					startupBackFileResult = t.executeCmds(cmds2);
					if (startupBackFileResult != null) {
						int beginIndex = startupBackFileResult.indexOf(cmds2[2]);
						int cmd2Len = beginIndex + cmds2[2].length() + 2;
						if (beginIndex > -1 && (startupBackFileResult.length() > cmd2Len))
							startupBackFileResult = startupBackFileResult.substring(cmd2Len);
					}
				}
				if (temp_result != null)
					temp_result = temp_result.replaceAll("  ---- more ----", "").replaceAll("42d", "").replaceAll("                                          ", "").replaceAll("\\\\[", "");
				if (runBackFileResult != null)
					runBackFileResult = runBackFileResult.replaceAll("  ---- more ----", "").replaceAll("42d", "").replaceAll("                                          ", "").replaceAll("\\\\[", "");
				if (startupBackFileResult != null)
					startupBackFileResult = startupBackFileResult.replaceAll("  ---- more ----", "").replaceAll("42d", "").replaceAll("                                          ", "").replaceAll("\\\\[", "");

			} catch (Exception e) {
				SysLogger.error("SSHUtil executeCmds方法执行失败！", e);
			} finally {
				if (t != null)
					t.disconnect();
			}

		}
		if (isSuccess) {
			if ("run".equals(bkpType) || "startup".equals(bkpType)) {
				NetCfgFileManager.getInstance().writeFileAndToDb(bkpType, fileName, fileDesc, bkpDate, vo, temp_result);
			} else {// bkpType = all
				NetCfgFileManager.getInstance().writeFileAndToDb("run", fileName, fileDesc, bkpDate, vo, runBackFileResult);
				String startupFileName = "";
				if (fileName.lastIndexOf(".") != -1) {
					startupFileName = fileName.substring(0, fileName.lastIndexOf(".")).concat("(2)").concat(fileName.substring(fileName.lastIndexOf(".")));
				} else {
					startupFileName = startupFileName.concat("(2)");
				}
				NetCfgFileManager.getInstance().writeFileAndToDb("startup", startupFileName, fileDesc, bkpDate, vo, startupBackFileResult);
			}
		}
	}
}

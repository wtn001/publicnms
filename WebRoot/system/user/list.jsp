<%@page language="java" contentType="text/html;charset=GB2312"%>
<%@page import="com.afunms.system.model.User"%>
<%@page import="com.afunms.common.base.JspPage"%>
<%@page import="java.util.List"%>
<%@page import="com.afunms.system.util.UserView"%>
<%@page import="com.afunms.common.util.SessionConstant"%>
<%@ include file="/include/globe.inc"%>

<%
	String rootPath = request.getContextPath();
	List list = (List) request.getAttribute("list");
	JspPage jp = (JspPage) request.getAttribute("page");

	UserView view = new UserView();

	//从session 中获取用户

	User operator = (User) session
			.getAttribute(SessionConstant.CURRENT_USER);
%>
<%
	String menuTable = (String) request.getAttribute("menuTable");
%>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=gb2312">
		<script language="JavaScript" type="text/javascript"
			src="<%=rootPath%>/include/navbar.js"></script>
		<script type="text/javascript" src="<%=rootPath%>/resource/js/page.js"></script>

		<link href="<%=rootPath%>/resource/<%=com.afunms.common.util.CommonAppUtil.getSkinPath() %>css/global/global.css"
			rel="stylesheet" type="text/css" />
<script type="text/javascript" src="<%=rootPath%>/js/jquery/jquery-1.7.2.min.js"></script>
		<script language="JavaScript" type="text/javascript">
  var listAction = "<%=rootPath%>/user.do?action=list";
  var delAction = "<%=rootPath%>/user.do?action=delete";
  var curpage= <%=jp.getCurrentPage()%>;
  var totalpages = <%=jp.getPageTotal()%>;

  function toAdd()
  {
     mainForm.action = "<%=rootPath%>/user.do?action=ready_add";
     mainForm.submit();
  }
</script>
		<script language="JavaScript" type="text/JavaScript">
var show = true;
var hide = false;
//修改菜单的上下箭头符号
function my_on(head,body)
{
	var tag_a;
	for(var i=0;i<head.childNodes.length;i++)
	{
		if (head.childNodes[i].nodeName=="A")
		{
			tag_a=head.childNodes[i];
			break;
		}
	}
	tag_a.className="on";
}
function my_off(head,body)
{
	var tag_a;
	for(var i=0;i<head.childNodes.length;i++)
	{
		if (head.childNodes[i].nodeName=="A")
		{
			tag_a=head.childNodes[i];
			break;
		}
	}
	tag_a.className="off";
}
//添加菜单	
function initmenu()
{
	var idpattern=new RegExp("^menu");
	var menupattern=new RegExp("child$");
	var tds = document.getElementsByTagName("div");
	for(var i=0,j=tds.length;i<j;i++){
		var td = tds[i];
		if(idpattern.test(td.id)&&!menupattern.test(td.id)){					
			menu =new Menu(td.id,td.id+"child",'dtu','100',show,my_on,my_off);
			menu.init();		
		}
	}

}
$(document).ready(function(){
$.ajax({
		   type: "POST",
		   url: "controller/groupQuery.action",
		   async:false,
		   success: function(obj){
			
		   }
		});
});

</script>
	</head>
	<body id="body" class="body" onload="initmenu();">
		<form id="mainForm" method="post" name="mainForm">
			<table id="body-container" class="body-container">
				<tr>
					<td class="td-container-menu-bar">
						<table id="container-menu-bar" class="container-menu-bar">
							<tr>
								<td>
									<%=menuTable%>
								</td>	
							</tr>
						</table>
					</td>
					<td class="td-container-main">
						<table id="container-main" class="container-main">
							<tr>
								<td class="td-container-main-content">
									<table id="container-main-content" class="container-main-content">
										<tr>
											<td>
												<table id="content-header" class="content-header">
								                	<tr>
									                	<td align="left" width="5"><img src="<%=rootPath%>/common/images/right_t_01.jpg" width="5" height="29" /></td>
									                	<td class="content-title"> 系统管理 >> 用户管理 >> 用户列表 </td>
									                    <td align="right"><img src="<%=rootPath%>/common/images/right_t_03.jpg" width="5" height="29" /></td>
									       			</tr>
									        	</table>
		        							</td>
										</tr>
										<tr>
											<td>
												<table id="content-body" class="content-body">
													<tr>
														<td>
															<table>
																<tr>
																	<td class="body-data-title" style="text-align: right;">
																		<a href="#" onclick="toAdd()">添加</a>
																		<a href="#" onclick="toDelete()">删除</a>&nbsp;&nbsp;&nbsp;
																	</td>
																</tr>
															</table>
														</td>
													</tr>
													<tr>
														<td>
															<table>
																<tr>
																	<td align="center" class="body-data-title">
																		<INPUT type="checkbox" class=noborder name="checkall"
																			onclick="javascript:chkall()">
																	</td>
																	<td align="center" class="body-data-title">
																		序号
																	</td>
																	<td align="center" class="body-data-title">
																		帐号
																	</td>
																	<td align="center" class="body-data-title">
																		姓名
																	</th>
																	<td align="center" class="body-data-title">
																		性别
																	</th>
																	<td align="center" class="body-data-title">
																		部门
																	</th>
																	<td align="center" class="body-data-title">
																		职务
																	</td>
																	<td align="center" class="body-data-title">
																		角色
																	</td>
																	<td align="center" class="body-data-title">
																		编辑
																	</td>
																</tr>
																<%
																	String sex = "";
																	User vo = null;
																	int startRow = jp.getStartRow();
																	for (int i = 0; i < list.size(); i++) {
																		vo = (User) list.get(i);
																		if (vo.getSex() == 1)
																			sex = "男";
																		else
																			sex = "女";
						
																		boolean flgadmin = false;
																		if (operator.getRole() == 0 && vo.getRole() == 0) {
						
																			flgadmin = true;
																		}
						
																		if (flgadmin) {
																%>
																<tr <%=onmouseoverstyle%>>
																	<td align="center" class="body-data-list">
																		<INPUT type="checkbox" class=noborder name=checkbox
																			value="<%=vo.getId()%>">
																	</td>
																	<td align="center" class="body-data-list">
																		<font color='blue'><%=startRow + i%></font>
																	</td>
																	<a
																		href="<%=rootPath%>/user.do?action=read&id=<%=vo.getId()%>">
																	<td align="center" class="body-data-list">
																		<a href="#" style="cursor: hand"
																			onclick="window.showModalDialog('<%=rootPath%>/user.do?action=read&id=<%=vo.getId()%>',window,',dialogHeight:400px;dialogWidth:600px')"><%=vo.getUserid()%>
																	</td>
																	</a>
																	<td align="center" class="body-data-list"><%=vo.getName()%></td>
																	<td align="center" class="body-data-list"><%=sex%></td>
																	<td align="center" class="body-data-list"><%=view.getDept(vo.getDept())%></td>
																	<td align="center" class="body-data-list"><%=view.getPosition(vo.getPosition())%></td>
																	<td align="center" class="body-data-list"><%=view.getRole(vo.getRole())%></td>
																	<td align="center" class="body-data-list">
																		<a
																			href="<%=rootPath%>/user.do?action=ready_edit&id=<%=vo.getId()%>"><img
																				src="<%=rootPath%>/resource/image/editicon.gif" border="0" />
																		</a>
																	</td>
																</tr>
																<%
																	}
						
																		if (!flgadmin && vo.getRole() != 0) {
																%>
																<tr <%=onmouseoverstyle%>>
																	<td align="center" class="body-data-list">
																		<INPUT type="checkbox" class=noborder name=checkbox
																			value="<%=vo.getId()%>">
																	</td>
																	<td align="center" class="body-data-list">
																		<font color='blue'><%=startRow + i%></font>
																	</td>
																	<a
																		href="<%=rootPath%>/user.do?action=read&id=<%=vo.getId()%>">
																	<td align="center" class="body-data-list">
																		<a href="#" style="cursor: hand"
																			onclick="window.showModalDialog('<%=rootPath%>/user.do?action=read&id=<%=vo.getId()%>',window,',dialogHeight:400px;dialogWidth:600px')"><%=vo.getUserid()%>
																	</td>
																	</a>
																	<td align="center" class="body-data-list"><%=vo.getName()%></td>
																	<td align="center" class="body-data-list"><%=sex%></td>
																	<td align="center" class="body-data-list"><%=view.getDept(vo.getDept())%></td>
																	<td align="center" class="body-data-list"><%=view.getPosition(vo.getPosition())%></td>
																	<td align="center" class="body-data-list"><%=view.getRole(vo.getRole())%></td>
																	<td align="center" class="body-data-list">
																		<a
																			href="<%=rootPath%>/user.do?action=ready_edit&id=<%=vo.getId()%>"><img
																				src="<%=rootPath%>/resource/image/editicon.gif" border="0" />
																		</a>
																	</td>
																</tr>
																<%
																	}
						
																	}
																%>
															</table>
														</td>
													</tr>
												</table>
											</td>
										</tr>
										<tr>
		        							<td>
		        								<table id="content-footer" class="content-footer">
		        									<tr>
		        										<td>
		        											<table width="100%" border="0" cellspacing="0" cellpadding="0">
									                  			<tr>
									                    			<td align="left" valign="bottom"><img src="<%=rootPath%>/common/images/right_b_01.jpg" width="5" height="12" /></td>
									                    			<td></td>
									                    			<td align="right" valign="bottom"><img src="<%=rootPath%>/common/images/right_b_03.jpg" width="5" height="12" /></td>
									                  			</tr>
									              			</table>
		        										</td>
		        									</tr>
		        								</table>
		        							</td>
		        						</tr>
		        					</table>
		        				</td>
		        			</tr>
						</table>
					</td>
				</tr>
			</table>
		</form>
	</BODY>
</HTML>

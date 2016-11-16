<%@ page contentType="text/html; charset=utf-8" language="java"
         import="org.ecocean.servlet.ServletUtilities,org.ecocean.CommonConfiguration" %>

<%

String context="context0";
context=ServletUtilities.getContext(request);
  //handle some cache-related security
  response.setHeader("Cache-Control", "no-cache"); //Forces caches to obtain a new copy of the page from the origin server
  response.setHeader("Cache-Control", "no-store"); //Directs caches not to store the page under any circumstance
  response.setDateHeader("Expires", 0); //Causes the proxy cache to see the page as "stale"
  response.setHeader("Pragma", "no-cache"); //HTTP 1.0 backward compatibility


%>

<jsp:include page="../header.jsp" flush="true" />

    <div class="container maincontent">
      <table border="0">
        <tr>
          <form id="adoption-form" style="display:none;" action="AdoptionAction" method="post" enctype="multipart/form-data" name="adoption_submission" target="_self" dir="ltr" lang="en">
        		<div class="form-header">
        			<h2>Adoption Profile</h2>
        			<img src="cust/mantamatcher/img/circle-divider.png"/>
        		</div>
        		<div class="input-col-1">
        			<div class="input-group">
        			  <span class="input-group-addon">Shark ID</span>
        			  <input id="sharkId" class=" input-m-width" name="shark" type="text" value="shark" placeholder="Browse the gallery and find the shark that suits you">  <%if (!shark.equals("")) { %>
        			    <a href="individuals.jsp?number<%=shark%>">Link</a> <%
        			      }
        			    %>
        			</div>
        			<%
        				if ((hasNickName == false )||(nick.equals(""))) {
        			%>
        			<div class="input-group">
        				<span class="input-group-addon">Shark Nickname</span>
        				<input class="input-l-width" type="text" name="newNickName" id="newNickName"></input>
        			</div>
        			<%
        				}
        			%>
        			<div class="input-group">
        			  <span class="input-group-addon">Adopter Name</span>
        			  <input class=" input-l-width" name="adopterName" type="text" value="<%=adopterName%>">
        			</div>
        			<div class="input-group">
        			  <span class="input-group-addon">Adopter Email</span>
        			  <input class=" input-l-width" name="adopterEmail" type="text" value="<%=adopterEmail%>"><br/>
        			</div>
        			<div class="input-group">
        			  <span class="input-group-addon">Address</span>
        			  <input class=" input-l-width" name="adopterAddress" type="text" value="<%=adopterAddress%>">
        			</div>
              </tr>
            </table>
          </td>
        </tr>
      </table>

    </div>


  <jsp:include page="../footer.jsp" flush="true"/>
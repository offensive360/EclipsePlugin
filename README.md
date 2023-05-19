<h1>OFFENSIVE360 SAST Eclipse Plugin</h1>
<h4>Plugin Requirements</h4>
<ul>
<li>Install a compatible Eclipse IDE</li> 
<li>Java 17.0.* is required</li>
</ul>

<h4>Plugin Installation</h4>
<ul>
<li>Goto <b>Help->Eclipse MarketPlace</b> to enter plugin marketplace.</li>
<li>Search for "O360 SAST" and install the plugin.</li>
<ul>
<li>If no plugin found in marketplace follow <a href="#manual">manual installation</a> steps</li>
</ul>
</ul>


<h4 id="manual">Plugin Manual Installation</h4>
<ul>
<li>Goto <b>Help-&gt;Install New Software</b> to enter plugin installation page</li>
<li>Now copy below link and paste it to the field</li>
<li> Plugin Update Site link <a href=""><bold>https://github.com/offensive360/EclipsePlugin/raw/main</bold></a></li>
<li>Now you will be able to see O360 SAST feature click on it and install</li>
<li>Uncheck check for updates checkbox before installing</li>
</ul>


<ul>
<h4>Update site zip file installation</h4>
<li>you can download this repo</li>
<li>After downloading follow above manual installation steps</li>
<li>Now instead of link click on <mark>Add > Local > Browse Update site file downloaded Zip file </mark></li>
</ul>


<h4>Server Configuration</h4>
In offensive360 bottom tool bar click on settings Icon or <mark>CTRL+ALT+D</mark> which will ask you server details.<br>
<ul>
<li> <b>SERVER_URL </b> : Host Address of the offensive360 api which will be provided by admin.</li>
<li><b>AUTH_TOKEN</b> :  Authentication Token provided by admin.</li>
<li>Invalid details will not allow a scan to run</li>
</ul>


<h4>Running A Scan </h4>
Now click on Scan Icon or <mark>CTRL+ALT+S</mark> which run scan on project source code and shows results.<br>
Right Click on File Popup Menu to run scan from there.<br>


<h4>Features</h4>
Right click on any vulnerability to get menu where you can<br>

1.<b>Go To Code</b> Double click on vulenrability to navigate to vulnerability <br>
2.<b>Suppress</b> False positive Vulnerabilities <br>
3.<b>Get Help</b> with references for the vulnerability <br>
4.<b>Clear All</b> vulnerabilities upon confirmation. <br>

<p>Let's find the vulnerabilities in one scan</p>

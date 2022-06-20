/*
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 *	Husqvarna AutoMower
 *
 *  Modified June 16, 2022
 *
 *  Instructions:
 *	Go to developer.husqvarnagroup.cloud
 *	- Sign in with your AutoConnect credentials
 *	- +Create application
 *		- Name it, URI for redirect https://cloud.hubitat.com/oauth/stateredirect
 *		- Connect this to authentication api and automower api
 *		- after saving, note application key and application secret to enter into settings here
 *
 *  Note: currently Husqvarna allows a total of 10K requests a month.  This app must poll so this must be taken into account
 *	This works out to shortest poll is every 5 minutes, with little remaining headroom
 */
//file:noinspection GroovyPointlessBoolean
//file:noinspection GroovyDoubleNegation
//file:noinspection GroovyUnusedAssignment
//file:noinspection GroovySillyAssignment
//file:noinspection unused
//file:noinspection GroovyVariableNotAssigned

// lgk june 2022 add human readable next run time
// also add lastupdate and mower statistics


import groovy.json.*
import groovy.transform.Field
import java.text.SimpleDateFormat

static String getVersionNum()		{ return "00.00.02" }
static String getVersionLabel()		{ return "Husqvarna Automower Manager, version "+getVersionNum() }
static String getMyNamespace()		{ return "imnotbob" }
static Integer getMinMinsBtwPolls()	{ return 3 }
static String getAutoMowerName()	{ return "Husqvarna AutoMower" }

@Field static final String sNULL	= (String)null
@Field static final String sBLANK	= ''
@Field static final String sSPACE	= ' '
@Field static final String sCLRRED	= 'red'
@Field static final String sCLRGRY	= 'gray'
@Field static final String sCLRORG	= 'orange'
@Field static final String sLINEBR	= '<br>'
@Field static final String sLOST	= 'lost'
@Field static final String sFULL	= 'full'
@Field static final String sBOOL	= 'bool'
@Field static final String sENUM	= 'enum'

definition(
	name:			"Husqvarna AutoMower Manager",
	namespace:		myNamespace,
	author:			"imnot_bob",
	description:	"Connect your Husqvarna AutoMowers, along with a Suite of Helper Apps.",
	category:		"Integrations",
	iconUrl:		"https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url:		"https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url:		"https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	importUrl:		"https://raw.githubusercontent.com/imnotbob/AutoMower/master/automower-connect.groovy",
	singleInstance:	true,
	oauth:			true
)

preferences {
	page(name: "mainPage")
	page(name: "removePage")
	page(name: "authPage")
	page(name: "mowersPage")
	page(name: "preferencesPage")

	page(name: "debugDashboardPage")
	page(name: "refreshAuthTokenPage")
}

mappings {
	path("/oauth/initialize"){action: [GET: "oauthInitUrl"]}
	path("/callback"){action: [GET: "callback"]}
	path("/oauth/callback"){action: [GET: "callback"]}
}


def mainPage(){
	String version=getVersionLabel()
	Boolean deviceHandlersInstalled
	Boolean readyToInstall //=false

	deviceHandlersInstalled=testForDeviceHandlers()
	readyToInstall=deviceHandlersInstalled

	dynamicPage(name: "mainPage", title: pageTitle(version.replace('er, v',"er\nV")), install: readyToInstall, uninstall: false, submitOnChange: true){

		// If no device Handlers we cannot proceed
		if(!(Boolean)state.initialized && !deviceHandlersInstalled){
			section(){
				paragraph "ERROR!\n\nYou MUST add the ${getAutoMowerName()} Device Handlers to the IDE BEFORE running setup."
			}
		}else{
			readyToInstall=true
		}

		if((Boolean)state.initialized && !(String)state.authToken){
			section(){
				paragraph(getFormat("warning", "You are no longer connected to the Husqvarna API. Please re-Authorize below."))
			}
		}

		if((String)state.authToken && !(Boolean)state.initialized){
			section(){
				paragraph "Please 'click \'Done\'' to save your credentials. Then re-open the AutoMower Manager to continue the setup."
			}
		}

		if((String)state.authToken && (Boolean)state.initialized){
			if(((List<String>)settings.mowers)?.size() > 0){
/*				section(sectionTitle("Helpers")){
					href ("helperAppsPage", title: inputTitle("Helper Applications"), description: "'Click' to manage Helper 'Applications'")
				}*/
			}
			section(sectionTitle("AutoMower Devices")){
				Integer howManyMowersSel=((List<String>)settings.mowers)?.size() ?: 0
				Integer howManyMowers=state.numAvailMowers ?: 0

				// Mowers
				href ("mowersPage", title: inputTitle("Mowers"), description: "'Click' to select AutoMowers [${howManyMowersSel}/${howManyMowers}]")
			}
		}

		section(sectionTitle("Preferences")){
			href ("preferencesPage", title: inputTitle("AutoMower Preferences"), description: "'Click' to manage global Preferences")
		}

		String authDesc=((String)state.authToken) ? "[Connected]\n" :"[Not Connected]\n"
		section(sectionTitle("Authentication")){
			href ("authPage", title: inputTitle("AutoMower API Authorization"), description: "${authDesc}'Click' for AutoMower Authentication")
		}
		if( debugLevel(5) ){
			section (sectionTitle("Debug Dashboard")){
				href ("debugDashboardPage", description: "${HE?'Click':'Tap'} to enter the Debug Dashboard", title: inputTitle("Debug Dashboard"))
			}
		}
		section(sectionTitle( "Removal")){
			href ("removePage", description: "'Click' to remove ${cleanAppName((String)app.label?:(String)app.name)}", title: inputTitle("Remove AutoMower Manager"))
		}

		section (sectionTitle("Naming")){
			String defaultName="AutoMower Manager"
			String defaultLabel
			if(!(String)state.appDisplayName){
				defaultLabel=defaultName
				app.updateLabel(defaultName)
				state.appDisplayName=defaultName
			}else{
				defaultLabel=(String)state.appDisplayName
			}
			label(name: "name", title: inputTitle("Assign a name"), required: false, defaultValue: defaultLabel, submitOnChange: true, width: 6)
			if(!app.label){
				app.updateLabel(defaultLabel)
				state.appDisplayName=defaultLabel
			}else{
				state.appDisplayName=(String)app.label
			}

			if(((String)app.label).contains('<span')){
				if((String)state.appDisplayName && !((String)state.appDisplayName).contains('<span ')){
					app.updateLabel((String)state.appDisplayName)
				}else{
					String myLabel=((String)app.label).substring(0, ((String)app.label).indexOf('<span'))
					state.appDisplayName=myLabel
					app.updateLabel((String)state.appDisplayName)
				}
			}
		}

		section(){
			paragraph(getFormat("line")+"<div style='color:#5BBD76;text-align:center'>${getVersionLabel()}<br>")
		}
	}
}

def removePage(){
	dynamicPage(name: "removePage", title: pageTitle("AutoMower Manager\nRemove AutoMower Manager and its Children"), install: false, uninstall: true){
		section (){
			paragraph(getFormat("warning", "Removing AutoMower Manager also removes all Mower automations and Devices!"))
		}
	}
}

Boolean initializeEndpoint(Boolean disableRetry=false) {
	String accessToken=(String)state.accessToken
	if(!accessToken){
		try {
			accessToken=createAccessToken()
		} catch(Exception e){
			LOG("authPage() --> No OAuth Access token", 3, sERROR, e)
		}
		if(!accessToken && !disableRetry){
			enableOauth()
			return initializeEndpoint(true)
		}
	}
	return (!!(String)state.accessToken)
}

// try to enable oauth on HE for this app
private void enableOauth(){
	Map params=[
		uri: "http://localhost:8080/app/edit/update?_action_update=Update&oauthEnabled=true&id=${app.appTypeId}".toString(),
		headers: ['Content-Type':'text/html;charset=utf-8']
	]
	try{
		httpPost(params){ resp ->
			//LogTrace("response data: ${resp.data}")
		}
	} catch (e){
		LOG("enableOauth something went wrong: ", 1, sERROR, e)
	}
}

// Setup OAuth between HE and Husqvarna clouds
def authPage(){
	LOG("authPage() --> Begin", 4, sTRACE)

	//log.debug "accessToken: ${state.accessToken}, ${state.accessToken}"

	Boolean success=initializeEndpoint()
	if(!success) {
		if(!state.accessToken){
			LOG("authPage() --> OAuth", 1, sERROR, e)
			LOG("authPage() --> Probable Cause: OAuth not enabled in Hubitat IDE for the 'AutoMower Manager' 'App'", 1, sWARN)
			LOG("authPage() --> No OAuth Access token", 3, sERROR)
			return dynamicPage(name: "authPage", title: pageTitle("AutoMower Manager\nOAuth Initialization Failure"), nextPage: sBLANK, uninstall: true){
				section(){
					paragraph "Error initializing AutoMower Authentication: could not get the OAuth access token.\n\nPlease verify that OAuth has been enabled in " +
							"the Hubitat IDE for the 'AutoMower Manager' 'App', and then try again.\n\nIf this error persists, view Live Logging in the IDE for " +
							"additional error information."
				}
			}
		}
	}

	String description=sBLANK
	Boolean uninstallAllowed=false
	Boolean oauthTokenProvided=false

	if((String)state.authToken){
		description="You are connected. Click Next/Done below."
		uninstallAllowed=true
		oauthTokenProvided=true
		apiRestored()
	}else{
		description="'Click' to enter AutoMower Credentials"
	}
	// HE OAuth process
	String redirectUrl=oauthInitUrl()

	// get rid of next button until the user is actually auth'd
	if(!oauthTokenProvided){
		LOG("authPage() --> Valid 'HE' OAuth Access token (${state.accessToken}), need AutoMower OAuth token", 3, sTRACE)
		LOG("authPage() --> RedirectUrl=${redirectUrl}", 5, sINFO)
		return dynamicPage(name: "authPage", title: pageTitle("AutoMower Manager\nHusqvarna API Authentication"), nextPage: sBLANK, uninstall: uninstallAllowed){
			oauthSection()
			if(getHusqvarnaApiKey() && getHusqvarnaApiSecret()) {
				section(sectionTitle(" ")){
					paragraph "'Click' below to log in to the Husqvarna service and authorize AutoMower Manager for Hubitat access. Be sure to 'Click' the 'Allow' button on the 2nd page."
					href url: redirectUrl, style: "external", required: true, title: inputTitle("AutoConnect Account Authorization"), description: description
				}
			}
		}
	}else{
		LOG("authPage() --> Valid OAuth token (${(String)state.authToken})", 3, sTRACE)
		return dynamicPage(name: "authPage", title: pageTitle("AutoMower Manager\nHusqvarna API Authentication"), nextPage: "mainPage", uninstall: uninstallAllowed){
			oauthSection()
			if(getHusqvarnaApiKey() && getHusqvarnaApiSecret()) {
				section(sectionTitle(" ")){
					paragraph "Return to the main menu"
					href url:redirectUrl, style: "embedded", state: "complete", title: inputTitle("AutoConnect Account Authorization"), description: description
				}
			}
		}
	}
}

def oauthSection(){
	section(sectionTitle("Husqvarna Oauth credentials")){
		paragraph "Enter Oauth you created from Husqvarna Development portal"
		input(name: "apiKey", title:inputTitle("Enter Oauth Key"), type: "text", required:true, description: "Tap to choose", submitOnChange: true, width: 6)
		input(name: "apiSecret", title:inputTitle("Enter Oauth Secret"), type: "text", required:true, description: "Tap to choose", submitOnChange: true, width: 6)
		String msg= """
  Instructions:
	Go to developer.husqvarnagroup.cloud
	- Sign in with your AutoConnect credentials
	- +Create application
		- Name it, redirect URI should be set https://cloud.hubitat.com/oauth/stateredirect
		- Connect this to authentication api and automower api
		- after saving, note application key and application secret to enter into settings here """
		paragraph msg
	}
}

def mowersPage(params){
	LOG("=====> mowersPage() entered", 5)
	Map mowers=getAutoMowers(true, "mowersPage")

	LOG("mowersPage() -> mower list: ${mowers}",5)
	LOG("mowersPage() starting settings: ${settings}",5)
	LOG("mowersPage() params passed: ${params}", 5, sTRACE)

	dynamicPage(name: "mowersPage", title: pageTitle("AutoMower Manager\nMowers"), params: params, nextPage: sBLANK, content: "mowersPage", uninstall: false){
		section(title: sectionTitle("Mower Selection")){
			if(mowers) {
				paragraph("'Click' below to see the list of AutoMowers available in your AutoConnect account and select the ones you want to connect.")
				LOG("mowersPage(): state.settingsCurrentMowers=${state.settingsCurrentMowers}	mowers=${(List<String>)settings.mowers}", 4, sTRACE)
				if((List<String>)state.settingsCurrentMowers != (List<String>)settings.mowers){
					LOG("state.settingsCurrentMowers != mowers: changes detected!", 4, sTRACE)
					state.settingsCurrentMowers=(List<String>)settings.mowers ?: []
					checkPolls('mowersPage ', true, false)
				}else{
					LOG("state.settingsCurrentMowers == mowers: No changes detected!", 4, sTRACE)
				}
				input(name: "mowers", title:inputTitle("Select Mowers"), type: sENUM, required:false, multiple:true, description: "Tap to choose", params: params,
						options: mowers, submitOnChange: true, width: 6)
			} else paragraph("No mowers found to connect.")
		}
	}
}

def preferencesPage(){
	LOG("=====> preferencesPage() entered. settings: ${settings}", 5)

	dynamicPage(name: "preferencesPage", title: pageTitle("AutoMower Manager\nPreferences"), nextPage: sBLANK){
		List echo=[]
		section(title: sectionTitle("Notifications")){
			paragraph("Notifications are only sent when the AutoConnect API connection is lost and unrecoverable, at most 1 per hour.", width: 8)
		}
		section(title: smallerTitle("Notification Devices")){
			input(name: "notifiers", type: "capability.notification", multiple: true, title: inputTitle("Select Notification Devices"), submitOnChange: true, width: 6,
					required: false /*(!settings.speak || ((settings.musicDevices == null) && (settings.speechDevices == null)))*/)
			if(settings.notifiers){
				echo=settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
				if(echo){
					input(name: "echoAnnouncements", type: sBOOL, title: "Use ${echo.size()>1?'simultaneous ':''}Announcements for the Echo Speaks device${echo.size()>1?'s':''}?",
							defaultValue: false, submitOnChange: true)
				}
			}
		}
		section(hideWhenEmpty: (!settings.speechDevices && !settings.musicDevices), title: smallerTitle("Speech Devices")){
			input(name: "speak", type: sBOOL, title: inputTitle("Speak messages?"), required: !settings?.notifiers, defaultValue: false, submitOnChange: true, width: 6)
			if((Boolean)settings.speak){
				input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: inputTitle("Select speech devices"),
						multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
				input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: inputTitle("Select music devices"),
						multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
				input(name: "volume", type: "number", range: "0..100", title: inputTitle("At this volume (%)"), defaultValue: 50, required: false, width: 4)
			}
		}
		if(echo || settings.speak){
			section(smallerTitle("Do Not Disturb")){
				input(name: "speakModes", type: "mode", title: inputTitle('Only speak notifications during these Location Modes:'), required: false, multiple: true, submitOnChange: true, width: 6)
				input(name: "speakTimeStart", type: "time", title: inputTitle('Only speak notifications<br>between...'), required: (settings.speakTimeEnd != null), submitOnChange: true, width: 3)
				input(name: "speakTimeEnd", type: "time", title: inputTitle("<br>...and"), required: (settings.speakTimeStart != null), submitOnChange: true, width: 3)
				String nowOK=((List)settings.speakModes || ((settings.speakTimeStart != null) && (settings.speakTimeEnd != null))) ?
						(" - with the current settings, notifications WOULD ${notifyNowOK()?sBLANK:'NOT '}be spoken now") : sBLANK
				paragraph(getFormat('note', "If both Modes and Times are set, both must be true" + nowOK))
			}
		}
		section(title: sectionTitle("Configuration")){}
		section(title: smallerTitle("Polling Interval")){
			paragraph("How frequently do you want to poll the Husqvarna cloud for changes? For maximum responsiveness to commands, it is recommended to set this to 1 minute.", width: 8)
			paragraph(sBLANK, width: 4)
			input(name: "pollingInterval", title:inputTitle("Select Polling Interval")+" (minutes)", type: sENUM, required:false, multiple:false, defaultValue:"15", description: "in Minutes", width: 4,
					options:["6", "10", "15", "30", "60"])
			if(settings.pollingInterval == null){ app.updateSetting('pollingInterval', "15") }
		}
		section(title: sectionTitle("Operations")){}
		section(title: smallerTitle("Debug Log Level")){
			paragraph("Select the debug logging level. Higher levels send more information to IDE Live Logging. A setting of 2 is recommended for normal operations.", width: 8)
			paragraph(sBLANK, width: 4)
			input(name: "debugLevel", title:inputTitle("Select Debug Log Level"), type: sENUM, required:false, multiple:false, defaultValue:"2", description: "2",
					options:["5", "4", "3", "2", "1", "0"], width: 4)
			if(settings.debugLevel == null){ app.updateSetting('debugLevel', "2") }
			generateEventLocalParams() // push down to devices
		}
	}
}

def debugDashboardPage(){
	LOG("=====> debugDashboardPage() entered.", 5)

	dynamicPage(name: "debugDashboardPage", title: sBLANK){
		section(getVersionLabel()){}
		section(sectionTitle("Commands")){
			href(name: "refreshAuthTokenPage", title: sBLANK, required: false, page: "refreshAuthTokenPage", description: "Tap to execute: refreshAuthToken()")
		}

		section(sectionTitle("Settings Information")){
			paragraph "debugLevel: ${getIDebugLevel()}"
			paragraph "pollingInterval (Minutes): ${getPollingInterval()}"
			paragraph "Selected Mowers: ${settings.mowers}"
		}
		section(sectionTitle("Dump of Debug Variables")){
			Map debugParamList=getDebugDump()
			LOG("debugParamList: ${debugParamList}", 4, sDEBUG)
			//if( debugParamList?.size() > 0 ){
			if( debugParamList != null ){
				debugParamList.each { key, value ->
					LOG("Adding paragraph: key:${key} value:${value}", 5, sTRACE)
					paragraph "${key}: ${value}"
				}
			}
		}
		section(sectionTitle("Commands")){
			href ("removePage", description: "Tap to remove AutoMower Manager ", title: sBLANK)
		}
	}
}


def refreshAuthTokenPage(){
	LOG("=====> refreshAuthTokenPage() entered.", 5)
	Boolean a=refreshAuthToken('refreshAuthTokenPage')

	dynamicPage(name: "refreshAuthTokenPage", title: sBLANK){
		section(){
			paragraph "refreshAuthTokenPage() was called"
		}
	}
}

Boolean testForDeviceHandlers(){
	// Only create the dummy devices if we aren't initialized yet
	if((Boolean)state.runTestOnce != null){
		if((Boolean)state.runTestOnce == false){
			List myChildren=(List)getAllChildDevices()
			if(myChildren) removeChildDevices( myChildren, true )	// Delete any leftover dummy (test) children
			state.runTestOnce=null
			return false
		}else{
			return true
		}
	}

	String DNIAdder=now().toString()
	String d1Str="dummyMowerDNI-${DNIAdder}"
	def d1
	Boolean success=false
	List myChildren=(List)getAllChildDevices()
	if(myChildren.size() > 0) removeChildDevices( myChildren, true )	// Delete my test children
	LOG("testing for device handlers", 4, sTRACE)
	try {
		d1=addChildDevice(myNamespace, getAutoMowerName(), d1Str, ((List)location.hubs)[0]?.id, ["label":"AutoMower:TestingForInstall", completedSetup:true])
		if((d1 != null) ) success=true
	} catch(Exception e){
		LOG("testForDeviceHandlers", 1, sERROR, e)
		if("${e}".startsWith("com.hubitat.app.exception.UnknownDeviceTypeException")){
			LOG("You MUST add the ${getAutoMowerName()} Device Handlers to the IDE BEFORE running the setup.", 1, sERROR)
		}
	}
	LOG("device handlers=${success}", 4, sINFO)
	Boolean deletedChildren=true
	try {
		if(d1) deleteChildDevice(d1Str)
	} catch(Exception e){
		LOG("Error deleting test devices (${d1})",1,sWARN, e)
		deletedChildren=false
	}

	if(!deletedChildren) runIn(5, delayedRemoveChildren, [overwrite: true])
	state.runTestOnce=success
	return success
}

void delayedRemoveChildren(){
	List myChildren=(List)getAllChildDevices()
	if(myChildren.size() > 0) removeChildDevices( myChildren, true )
}

void removeChildDevices(List devices, Boolean dummyOnly=false){
	if(!devices){
		return
	}
	Boolean first=true
	String devName=sNULL
	try {
		devices?.each {
			devName=it.displayName
			String devDNI=it.deviceNetworkId
			if(!dummyOnly || devDNI?.startsWith('dummy')){
				if(first) {
					first=false
					LOG("Removing ${dummyOnly?'test':'unused'} child devices",3,sTRACE)
				}
				LOG("Removing unused child: ${devDNI} - ${devName}",1,sWARN)
				deleteChildDevice(devDNI)
			}else{
				LOG("Keeping child: ${devDNI} - ${devName}",4,sTRACE)
			}
		}
	} catch(Exception e){
		LOG("Error removing device ${devName}",1,sWARN, e)
	}
}


static String getCallbackUrl()			{ return "https://cloud.hubitat.com/oauth/stateredirect" }
static String getMowerApiEndpoint()		{ return "https://api.amc.husqvarna.dev/v1" }
static String getApiEndpoint()			{ return "https://api.authentication.husqvarnagroup.dev/v1/oauth2" }
static String getWssEndpoint()			{ return "wss://ws.openapi.husqvarna.dev/v1"}

//String getBuildRedirectUrl()	{ return "${serverUrl}/oauth/stateredirect?access_token=${state.accessToken}" }
String getStateUrl()			{ return "${getHubUID()}/apps/${app?.id}/callback?access_token=${state?.accessToken}" }


String getHusqvarnaApiKey(){ return (String)settings.apiKey }
String getHusqvarnaApiSecret(){ return (String)settings.apiSecret }

// OAuth Init URL
String oauthInitUrl(){
	LOG("oauthInitUrl", 4)
	state.oauthInitState=getStateUrl() // HE does redirect a little differently
	//log.debug "oauthInitState: ${state.oauthInitState}"

	Map oauthParams=[
		response_type:	"code",
		client_id:	getHusqvarnaApiKey(),					// actually, the AutoMower Manager app's client ID
		scope:		"app",
		redirect_uri:	getCallbackUrl(),
		state:		state.oauthInitState
	]

	String res= getApiEndpoint()+"/authorize?${toQueryString(oauthParams)}"
	LOG("oauthInitUrl - location: ${res}", 4, sDEBUG)
	return res
}

void parseAuthResponse(resp){
	String msgH="Display http response | "
	//log.debug "response data: ${myObj(resp.data)} ${resp.data}"
	String str=sBLANK
	resp.data.each {
		str += "\n${it.key} --> ${it.value}, "
	}
	LOG(msgH+"response data: ${str}",4,sDEBUG)
	LOG(msgH+"response data object type: ${myObj(resp.data)}",4,sDEBUG)

	str=sBLANK
	resp.getHeaders().each {
		str += "\n${it.name}: ${it.value}, "
	}
	log.debug msgH+"response headers: ${str}"
	log.debug msgH+"isSuccess: ${resp.isSuccess()} | statucode: ${resp.status}"

	//str=sBLANK
	//log.debug "resp param ${resp.params}"
	//resp.params.each { str += "${it.name}: ${it.value}"}
	//log.debug "response params: ${str}"
}

private static String encodeURIComponent(value){
	// URLEncoder converts spaces to + which is then indistinguishable from any
	// actual + characters in the value. Match encodeURIComponent in ECMAScript
	// which encodes "a+b c" as "a+b%20c" rather than URLEncoder's "a+b+c"
	return URLEncoder.encode(
			"${value}".toString().replaceAll('\\+','__wc_plus__'),
			'UTF-8'
	).replaceAll('\\+','%20').replaceAll('__wc_plus__','+')
}

def callback(){
	LOG("callback()>> params: ${params}" /* params.code ${params.code}, params.state ${params.state}, state.oauthInitState ${state.oauthInitState}"*/, 4, sDEBUG)
	def code=params.code
	String oauthState=params.state
	String eMsg = sNULL

	if(oauthState == state.oauthInitState){
		LOG("callback() --> States matched!", 4)
		Map rdata=[
				grant_type: "authorization_code",
				code	: code,
				client_id : getHusqvarnaApiKey(),
				client_secret : getHusqvarnaApiSecret(),
				state	: oauthState,
				redirect_uri: callbackUrl,
		]

		//String tokenUrl=getApiEndpoint()+"/token?${toQueryString(tokenParams)}"
		String tokenUrl=getApiEndpoint()+"/token"
		String data=rdata.collect{ String k,v -> encodeURIComponent(k)+'='+encodeURIComponent(v) }.join('&')
		Map reqP=[
				uri: tokenUrl,
				query: null,
				contentType: "application/x-www-form-urlencoded",
//				requestContentType: "application/json",
				body: data,
				timeout: 30
		]
		LOG("callback()-->reqP ${reqP}", 4)
		try {
			httpPost(reqP){ resp ->
				if(resp && resp.data && resp.isSuccess()){
//					parseAuthResponse(resp)
					String kk
					resp.data.each { kk=it.key }
					Map ndata=(Map)new JsonSlurper().parseText(kk)
//					log.debug "ndata : ${ndata}"

					state.refreshToken=ndata.refresh_token
					state.authToken=ndata.access_token
					Long tt= (Long)now() + (ndata.expires_in * 1000)
					//log.error "tt is ${tt}"
					state.authTokenExpires=tt
					atomicState.refreshToken=ndata.refresh_token
					atomicState.authToken=ndata.access_token
					//atomicState.authTokenExpires=tt
					//log.error "state.authTokenExpires is ${state.authTokenExpires}"

					LOG("Expires in ${ndata.expires_in} seconds", 3)
					LOG("swapped token: $ndata; state.refreshToken: ${state.refreshToken}; state.authToken: ${(String)state.authToken}", 3)
					state.remove('oauthInitState')
					eMsg = success()
				} else { eMsg = fail() }
			}
		} catch(Exception e){
			LOG("auth callback()", 1, sERROR, e)
			//if(resp) parseAuthResponse(resp)
			eMsg = fail()
		}
	}else{
		LOG("callback() failed oauthState != state.oauthInitState", 1, sWARN)
		eMsg = fail()
	}
	render contentType: 'text/html', data: eMsg
}

String success(){
	String message="""
	<p>Your AutoConnect Account is now connected!</p>
	<p>Close this window and click 'Done' to finish setup.</p>
	"""
	return connectionStatus(message)
}

String fail(){
	String message="""
		<p>The connection could not be established!</p>
		<p>Close this window and click 'Done' to return to the menu.</p>
	"""
	return connectionStatus(message)
}

String connectionStatus(String message, Boolean close=false){
	String redirectHtml = close ? """<script>document.getElementsByTagName('html')[0].style.cursor = 'wait';setTimeout(function(){window.close()},2500);</script>""" : sBLANK
	/*String redirectHtml=sBLANK
	if(redirectUrl){
		redirectHtml="""
			<meta http-equiv="refresh" content="3; url=${redirectUrl}" />
		"""
	} */
	String hubIcon='https://raw.githubusercontent.com/SANdood/Icons/master/Hubitat/HubitatLogo.png'

	String html="""
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>Husqvarna connection</title>
<style type="text/css">
		@font-face {
				font-family: 'Swiss 721 W01 Thin';
				src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
				src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
						url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
						url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
						url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
				font-weight: normal;
				font-style: normal;
		}
		@font-face {
				font-family: 'Swiss 721 W01 Light';
				src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
				src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
						url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
						url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
						url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
				font-weight: normal;
				font-style: normal;
		}
		.container {
				width: 90%;
				padding: 4%;
				/*background: #eee;*/
				text-align: center;
		}
		img {
				vertical-align: middle;
		}
		p {
				font-size: 2.2em;
				font-family: 'Swiss 721 W01 Thin';
				text-align: center;
				color: #666666;
				padding: 0 40px;
				margin-bottom: 0;
		}
		span {
				font-family: 'Swiss 721 W01 Light';
		}
</style>
</head>
<body>
		<div class="container">
				<img src="https://raw.githubusercontent.com/imnotbob/autoMower/master/images/husqvarna-logo.png" alt="ecobee icon" />
				<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
				<img src="${hubIcon}" alt="Hubitat logo" />
				${message}
		</div>
	${redirectHtml}
</body>
</html>
""".toString()
	return html
}
/* """ */


static String myObj(obj){
	if(obj instanceof String){return 'String'}
	else if(obj instanceof Map){return 'Map'}
	else if(obj instanceof List){return 'List'}
	else if(obj instanceof ArrayList){return 'ArrayList'}
	else if(obj instanceof Integer){return 'Int'}
	else if(obj instanceof BigInteger){return 'BigInt'}
	else if(obj instanceof Long){return 'Long'}
	else if(obj instanceof Boolean){return 'Bool'}
	else if(obj instanceof BigDecimal){return 'BigDec'}
	else if(obj instanceof Float){return 'Float'}
	else if(obj instanceof Byte){return 'Byte'}
	else if(obj instanceof ByteArrayInputStream){return 'ByteArrayInputStream'}
	else{ return 'unknown'}
}

Boolean weAreLost(String msgH, String meth){
	String msg = sBLANK
	if(!(String)state.authToken) {
		apiLost(msgH+"weAreLost() found no auth token, called by ${meth}")
	}
	if(apiConnected() == sLOST){
		msg += "found connection lost to husqvarna | "
		if( refreshAuthToken(meth) ){
			msg += " - Was able to recover the lost connection. Please ignore any notifications received. | "
			LOG(msgH+msg, 4, sINFO)
		}else{
			msg += " - Unable to refresh token and get mowers do to loss of API Connection. Please ensure you are authorized."
			LOG(msgH+msg, 1, sERROR)
			return true
		}
	}
	return false
}

// Get the list of mowers for use in the settings pages
Map<String,String> getAutoMowers(Boolean frc=false, String meth="followup", Boolean isRetry=false){
	String msgH="getAutoMowers(force: $frc, calledby: $meth, isRetry: $isRetry) | "

	if(debugLevel(4)) { LOG(msgH+"====> entered ",4,sTRACE) }
	else LOG(msgH, 3,sTRACE)

	if(weAreLost(msgH, 'getAutoMowers')){
		return null
	}
	String cached=sBLANK
	Boolean skipIt=false
	Boolean myfrc=(!state.mowerData || !state.mowersWithNames)
	Integer lastU=getLastTsValSecs("getAutoUpdDt")
	if( (frc && lastU < 60)) { skipIt=true }
	if( (!frc && lastU < 150) ) { skiptIt=true } // related to getMinMinsBtwPolls
	Map<String, String> mowers=[:]
	Map mowersLocation=[:]

	String msg=sBLANK

	if(myfrc || !skipIt) {
		updTsVal("getAutoUpdDt")
		Map deviceListParams=[
			uri: getMowerApiEndpoint() +"/mowers",
			headers: [
				"Content-Type": "application/vnd.api+json",
				"Authorization": "Bearer ${(String)state.authToken}",
				"Authorization-Provider": "husqvarna",
				"X-Api-Key":getHusqvarnaApiKey()
			],
			query: null,
			timeout: 30
		]
		if(debugLevel(4)) {
			msg+="http params -- ${deviceListParams} "
		}
		msg +="HTTPGET "
		if(msg) {
			LOG(msgH + msg, 3, sTRACE)
			msg=sBLANK
		}

		try {
			httpGet(deviceListParams) { resp ->
				LOG(msgH + "httpGet() ${resp.status} Response", 4, sTRACE)
				String rdata
				Map adata
				if(resp) {
					rdata=resp.data.text // need to save first time since it is a ByteArrayInputStream
					if(rdata) adata=(Map)new JsonSlurper().parseText(rdata)
				}
				if(resp && resp.isSuccess() && resp.status == 200 && adata) {

					List<Map> ndata=((List<Map>)adata.data)?.findAll { it.type == "mower" }

					state.numAvailMowers=((List<Map>)ndata)?.size() ?: 0

					Map<String, Map> mdata=[:]
					ndata.each { Map mower ->
						String dni=getMowerDNI((String) mower.id)
						mowers[dni]=getMowerDisplayName(mower)
						mowersLocation[dni]=getMowerLocation(mower)
						mdata[dni]=mower
					}
					state.mowerData=mdata
					//log.debug "resp: ${state.mowerData}"
				}else{
					LOG(msgH + "httpGet() in else: http status: ${resp.status}", 1, sTRACE)
					//refresh the auth token
					if(resp.status == 500) { //} && resp.data?.status?.code == 14) {
						if(!isRetry){
							LOG(msgH + "Refreshing auth_token!", 3, sTRACE)
							if(refreshAuthToken('getAutoMowers')) return getAutoMowers(frc, meth, true)
						}
					}else{
						LOG(msgH + "Other error. Status: ${resp.status} Response data: ${rdata} ", 1, sERROR)
					}
					return [:]
				}
			}
		} catch(Exception e) {
			LOG(msgH + "___exception", 1, sERROR, e)
			if(!isRetry) {
				Boolean a = refreshAuthToken('getAutoMowers')
			}
			return [:]
		}
		state.mowersWithNames=mowers
		state.mowersLocation=mowersLocation
	}else{
		mowers= state.mowersWithNames
		mowersLocation=state.mowersLocation
		cached="cached "
	}
	msg += cached+"mowersWithNames: ${mowers}, locations: ${mowersLocation}"
	LOG(msgH+msg, 4, sTRACE)
	return (mowers) ? mowers.sort { it.value } : null
}

/*
 * max 1 command per second
 * Commands are queued at Husqvarna, and executed when mower checks in
 */
Boolean sendCmdToHusqvarna(String mowerId, Map data, Boolean isRetry=false, String uriend='actions') {
	String msgH = "sendCmdToHusqvarna(mower: $mowerId, data: $data, isRetry: $isRetry uriend: $uriend) | "

	Boolean ok = (mowerId && mowerId in (List<String>)settings.mowers)
	if (!ok) {
		LOG(msgH + "mower not enabled in settings: $settings.mowers", 1, sERROR)
		return false
	}

	if (debugLevel(4)) LOG(msgH + "===> entered", 4, sTRACE)
	else LOG(msgH, 3,sTRACE)

	if(weAreLost(msgH, 'sendCmdToHusqvarna')){
		return false
	}

	Map deviceListParams=[
		uri: getMowerApiEndpoint() +"/mowers"+"/${mowerId}/${uriend}",
		headers: [
			"Content-Type": "application/vnd.api+json",
			"Authorization": "Bearer ${(String)state.authToken}",
			"Authorization-Provider": "husqvarna",
			"X-Api-Key":getHusqvarnaApiKey()
		],
		query: null,
		body: new JsonOutput().toJson(data),
		timeout: 30
	]
	String msg
	msg = sBLANK
	if(debugLevel(4)) {
		msg+="http params -- ${deviceListParams} "
	}
	msg +="HTTPPOST "
	if(msg) {
		LOG(msgH + msg, 2, sTRACE)
		msg=sBLANK
	}

	try {
		httpPost(deviceListParams) { resp ->
			String rdata
			/*
			Map adata
			if(resp) {
				rdata=resp.data.text // need to save first time since it is a ByteArrayInputStream
				if(rdata) adata=(Map)new JsonSlurper().parseText(rdata)
			}*/
			if(resp && resp.isSuccess() && resp.status >= 200 && resp.status <= 299) {
				LOG(msgH + "httpPost() ${resp.status} Response", 2, sTRACE)
				runIn(85, poll, [overwrite: true]) // give time for command to complete; then get new status
			}else{
				LOG(msgH + "httpPost() in else: http status: ${resp.status}", 1, sTRACE)
				//refresh the auth token
				if(resp.status == 500) { //} && resp.data?.status?.code == 14) {
					//LOG(msgH + "Storing the failed action to try later", 1, sTRACE)
					//state.action="getAutoMowers"
					if(!isRetry){
						LOG(msgH + "Refreshing auth_token!", 3, sTRACE)
						if(refreshAuthToken('sendCmdToHusqvarna')) return sendCmdToHusqvarna(mowerId, data, true,uriend)
					}
				}else{
					LOG(msgH + "Other error. Status: ${resp.status} Response data: ${rdata} ", 1, sERROR)
				}
				return false
			}
		}
	} catch(Exception e) {
		LOG(msgH + "___exception", 1, sERROR, e)
		//state.action="getAutoMowers"
		if(!isRetry) {
			Boolean a = refreshAuthToken('sendCmdToHusqvarna')
		}
		return false
	}
	return true
}

Boolean sendSettingToHusqvarna(String mowerId, Map data, Boolean isRetry=false) {
	return sendCmdToHusqvarna(mowerId, data, isRetry,'settings')
}

Boolean sendScheduleToHusqvarna(String mowerId, Map data, Boolean isRetry=false) {
	return sendCmdToHusqvarna(mowerId, data, isRetry,'calendar')
}

Map getMowerMap(String tid) {
	if(tid) {
		String dni=getMowerDNI(tid)
		Map<String,Map>mowerMap=(Map<String,Map>)state.mowerData
		if(dni && mowerMap) {
			return mowerMap[dni]
		}
	}
	return null
}

String getMowerName(String tid){
	// Get the name for this mower
	String DNI=getMowerDNI(tid)
	Map<String,String> mowersWithNames=state.mowersWithNames
	String mowerName=(mowersWithNames?.containsKey(DNI)) ? mowersWithNames[DNI] : sBLANK
	if(mowerName == sBLANK){ mowerName=getChildDevice(DNI)?.displayName } // better than displaying 'null' as the name
	return mowerName
}

static String getMowerDNI(String tid){
	return tid
	//return 'autoconnect-mower-' + ([app.id.toString(), tid].join('.'))
}

static String getMowerDisplayName(Map mower){
	String nm=(String)mower?.attributes?.system?.name ?: 'Name not found'
	return nm + ' - ' + getMowerModelName(mower)
}

static String getMowerModelName(Map mower){
	return (String)mower?.attributes?.system?.model ?: "Model not found"
}

static String getMowerLocation(Map mower){
   
	if((String)mower?.attributes?.mower?.mode && (String)mower?.attributes?.mower?.activity) {
		return (String)mower?.attributes?.mower?.mode+sSPACE+(String)mower?.attributes?.mower?.activity
	}
	return "location not found??"
}

void installed(){
	LOG("Installed with settings: ${settings}",1,sTRACE)
	initialize()
}

void uninstalled(){
	LOG("Uninstalling...",0,sWARN)
	unschedule()
	unsubscribe()
	removeChildDevices( (List)getAllChildDevices(), false )	// delete all my children!
	// Child apps are supposedly automatically deleted.
}

void updated(){
	LOG("Updated with settings: ${settings}",2,sTRACE)
	unschedule()
	unsubscribe()
	cleanupStates()
	initialize()
}

void rebooted(evt){
	LOG("Hub rebooted, re-initializing", 2, sTRACE)
	initialize()
}

void settingUpdate(String name, value, String type=sNULL){
	if(name && type){
		app.updateSetting(name, [type: type, value: value])
	}
	else if(name && !type){ app.updateSetting(name, value) }
}

void settingRemove(String name){
	LOG("settingRemove($name)...",4, sTRACE)
	if(name && settings.containsKey(name)){ app?.removeSetting(name) }
}

void cleanupStates(){
	LOG("Cleaning up states", 3, sTRACE)
	remTsVal('getAutoUpdDt')
	state.remove('timeSendPush')
	state.remove('oauthInitState')
	state.remove('lastLOGerror')
	state.remove('LastLOGerrorDate')
	state.remove('sunriseTime')
	state.remove('sunsetTime')
	state.remove('timeOfDay')
	state.remove('initializedEpic')
	state.remove('action')

	state.remove('statLocation')
	state.remove('dbg_lastSunriseEvent')
	state.remove('dbg_lastSunriseEventDate')
	state.remove('dbg_lastSunsetEvent')
	state.remove('dbg_lastSunsetEventDate')

/*	state.remove("pollingInterval")
	settingRemove('arrowPause')
	state.remove('audio') */
}

@Field static final Integer iWATCHDOGINTERVAL=10	// In minutes
@Field static final Integer iREATTEMPTINTERVAL=30	// In seconds

@Field static Random randomSeed=new Random()

void initialize(){
	unsubscribe()
	unschedule()

	LOG("${getVersionLabel()} Initializing...", 2, sDEBUG)

	if(settings.pollingInterval == null){ app.updateSetting('pollingInterval', "15") }

	Integer tt=randomSeed.nextInt(100)	// get the random number generator going

	state.inPollChildren=true
	state.skipTime=now()

	if((String)state.authToken && (Boolean)state.initialized) {
		Long timeBeforeExpiry= state.authTokenExpires ? (Long)state.authTokenExpires - now() : 0
		if(timeBeforeExpiry > 0) {
			//state.connected=sFULL
			apiRestored(false)
		} else apiLost("initialize found expired token")
	} else state.connected=sWARN
	updateMyLabel()
	state.reAttempt=0
	state.remove('reAttempt')
	if(state.inTimeoutRetry){ state.inTimeoutRetry=0; state.remove('inTimeoutRetry') }

	Map updatesLog=[mowerUpdated:true, runtimeUpdated:true, forcePoll:true, getWeather:true, alertsUpdated:true, extendRTUpdated:true ]
	state.updatesLog=updatesLog

	state.numAvailMowers=0
	state.mowerData=[:]

	Map myMowers
	if((Boolean)state.initialized){
		myMowers=getAutoMowers(true, "initialize")
	}
	Boolean apiOk=(myMowers!=null)

	// Create children, This should only be needed during initial setup and when mowers or sensors are added or removed.
	Boolean aOK=apiOk
	aOK=(aOK && ((List<String>)settings.mowers)?.size() > 0)
	if(aOK) { aOK=createChildrenMowers() }
	if(aOK) deleteUnusedChildren()

	subscribe(location, "systemStart", rebooted)					// re-initialize if the hub reboots

	state.inPollChildren=false
	state.remove('inPollChildren')
	state.remove('skipTime')
	state.lastScheduledPoll=null

	if(aOK){ forceNextPoll() }

	// Schedule the various handlers
	checkPolls('initialize() ', apiOk, true)

	if(!(Boolean)state.initialized){
		state.initialized=true
		// These two below are for debugging and statistics purposes
		state.initializedEpoch=now()
		state.initializedDate=getDtNow() // getTimeStamp
	}

	//send activity feeds to tell devices connection status
	String notificationMessage=aOK ? "is connected" : (apiOk ? "had an error during setup of devices" : "api not connected")

	LOG("${getVersionLabel()} - initialization complete "+notificationMessage,2,sDEBUG)
	if(!state.versionLabel) state.versionLabel=getVersionLabel()
	if(aOK) runIn(8, poll, [overwrite: true])
}

Boolean createChildrenMowers(){
	Boolean result=true
	Integer ccnt=0
	Integer fnd=0
	LOG("createChildrenMowers() entered: mowers=${(List<String>)settings.mowers}", 4, sTRACE)
	// Create the child Mower Devices
	List devices=((List<String>)settings.mowers).collect { dni ->
		def d=getChildDevice(dni)
		if(!d){
			try {
				d=addChildDevice(myNamespace, getAutoMowerName(), dni, ((List)location.hubs)[0]?.id, ["label":"Mower: ${state.mowersWithNames[dni]}", completedSetup:true])
			} catch(Exception e){
				if("${e}".startsWith("com.hubitat.app.exception.UnknownDeviceTypeException")){
					LOG("You MUST add the ${getAutoMowerName()} Device Handler to the IDE BEFORE running the setup.", 1, sERROR, e)
					state.runTestOnce=null
					result=false
					return false
				}
			}
			ccnt += 1
			LOG("created ${d.displayName} with id ${dni}", 4, sTRACE)
		}else{
			fnd += 1
			LOG("found ${d.displayName} with id ${dni} already exists", 4, sTRACE)
		}
		return d
	}
	if(result) LOG("Created ($ccnt) / Updated ($fnd) / Total: ${devices.size()} mowers", 4, sTRACE)
	return result
}
/*
String getChildAppName(String childId){
	def child=getChildApps().find { it.id.toString() == childId }
	return child ? (cleanAppName((String)child.label?:(String)child.name)) : sBLANK
} */

static String cleanAppName(String name){
	if(name){
		String cleanName
		Integer idx=name.indexOf('<span')
		return ((idx > 0) ? name.substring(0, idx) : name).trim()
	}
	return sNULL
}

// NOTE: For this to work correctly getAutoMowers()
void deleteUnusedChildren(){
	LOG("deleteUnusedChildren() entered", 5, sTRACE)

	// Always make sure that the dummy devices were deleted
	removeChildDevices((List)getAllChildDevices(), true)		// Delete dummy devices

	if(((List<String>)settings.mowers)?.size() == 0){
		// No mowers, need to delete all children
		LOG("Deleting All My Children!", 0, sWARN)
		removeChildDevices((List)getAllChildDevices(), false)
	}else{
		// Only delete those that are no longer in the list
		// This should be a combination of any removed mowers and any removed sensors
		List allMyChildren=(List)getAllChildDevices()
		LOG("These are currently all of my children: ${allMyChildren}", 4, sDEBUG)

		// Don't delete any devices that are configured in settings (mowers)
		List childrenToKeep=((List<String>)settings.mowers ?: [])
		LOG("These are the children to keep around: ${childrenToKeep}", 4, sTRACE)

		List childrenToDelete=allMyChildren.findAll { !childrenToKeep.contains(it.deviceNetworkId) }
		if(childrenToDelete.size() > 0){
			LOG("Ready to delete these devices: ${childrenToDelete}", 0, sWARN)
			childrenToDelete?.each { deleteChildDevice(it.deviceNetworkId) }
		}
	}
}

void scheduledWatchdog(evt=null, Boolean local=false, String meth="schedule/runin"){
	String msgH="scheduledWatchdog() | "
	String evtStr=evt ? "${evt.name}:${evt.value}" : 'null'
	String msg="event: (${evtStr}) | local (${local}) | by ${meth} | "
	Boolean debugLevelFour=debugLevel(4)
	if(debugLevelFour) { LOG(msgH+msg, 4, sTRACE); msg=sBLANK }

	// Check to see if we have called too soon
	if(!state.lastScheduledWatchdog) state.lastScheduledWatchdog=now() - 3600001L
	Long oldLast=state.lastScheduledWatchdog
	Long timeSinceLastWatchdog=(now() - oldLast) / 60000L
	if( timeSinceLastWatchdog < 2L ){
		msg += "It has only been ${timeSinceLastWatchdog*60} seconds since last call. Exiting"
		if(debugLevelFour) LOG(msgH+msg, 4, sTRACE)
		return
	}

	// check if token needs to be refreshed
	Long texp = (Long)state.authTokenExpires
	Long timeBeforeExpiry=texp ? texp - now() : 0L
	if(timeBeforeExpiry < 1800000L){
		msg += "Calling refreshToken | timeBeforeExpiry: ${timeBeforeExpiry} | "
		if(debugLevelFour) { LOG(msgH+msg, 4, sTRACE); msg=sBLANK }
		if( !refreshAuthToken('scheduledWatchdog') ){
			return
		}
	}

	if(weAreLost(msgH, 'scheduledWatchdog')){
		msg += "exiting - no connection"
		if(debugLevelFour) { LOG(msgH+msg, 4, sTRACE); msg=sBLANK }
		return
	}

	if(msg && debugLevelFour) { LOG(msgH+msg, 4, sTRACE); msg=sBLANK }
	checkPolls(msgH)

	String oldLastS=state.lastScheduledWatchdogDate
	// Only update the Scheduled timestamp if run by a timer (schedule or runIn)
	if( (evt==null && !local) || !oldLast || !oldLastS ){
		state.lastScheduledWatchdog=now()
		state.lastScheduledWatchdogDate=getTimestamp()
		if(!oldLast) oldLast=now() - 3600001L

		if(now() > (oldLast+3600000L)){
			// do a forced update once an hour, just because (e.g., forces Hold Status to update completion date string)
			forceNextPoll()
			msg += "forcing device update | "
		}
	}

	if(msg && debugLevelFour) LOG(msgH+msg, 4, sTRACE)
}

void checkPolls(String msgH, Boolean apiOk=true, Boolean frc=false){
	Boolean haveMowers=(((List<String>)settings.mowers)?.size() > 0)
	if(apiOk && haveMowers) {
		if(frc) LOG("Spawning the poll scheduled event. (mowers.size(): ${((List<String>) settings.mowers)?.size()})", 2, sTRACE)
		if(frc || !isDaemonAlive("poll", msgH)) {
			LOG(msgH + "rescheduling poll daemon", 1, sTRACE); spawnDaemon("poll", !frc)
		}
	}else{
		unschedule(pollScheduled)
		if(frc && !haveMowers) LOG(msgH+"Not starting poll daemon; there are no mowers currently selected for use", 1, sWARN)
	}
	if(frc || !isDaemonAlive("watchdog", msgH)){ LOG(msgH+"rescheduling watchdog daemon",1,sTRACE); spawnDaemon("watchdog", !frc) }
}

Boolean isDaemonAlive(String daemon="all", String msgI){
	String msgH="isDaemonAlive(${daemon}, calledby: ${msgI}) | "
	String msg=sBLANK
	Boolean debugLevelFour=debugLevel(4)
	List<String> daemonList=["poll", "watchdog", "all"]

	Boolean result=true

	//if(debugLevelFour) LOG("isDaemonAlive() - now() == ${now()} for daemon (${daemon})", 1, sTRACE)

	if(daemon == "poll" || daemon == "all"){
		Integer pollingInterval=getPollingInterval()
		Map resM=checkT('poll', (Long)state.lastScheduledPoll, pollingInterval)
		msg += resM.msg ?: sBLANK
		result=resM.res && result
	}

	if(daemon == "watchdog" || daemon == "all"){
		Map resM=checkT('watchdog', (Long)state.lastScheduledWatchdog, iWATCHDOGINTERVAL)
		msg += resM.msg ?: sBLANK
		result=resM.res && result
	}

	if(!daemonList.contains(daemon) ){
		msg += " - Unknown daemon: ${daemon} received. Do not know how to check this daemon."
		LOG(msgH+msg, 1, sERROR)
		result=false
		return result
	}
	msg += " - result is ${result}"
	if(result && debugLevelFour) LOG(msgH+msg, 4, sTRACE)
	if(!result) LOG(msgH+msg, 1, sWARN)
	return result
}

Map checkT(String typ, Long lVal, Integer intervalMins){
	Boolean result=true
	Long lastScheduled=lVal
	Long timeSinceLastMins=!lastScheduled ? 1000L : ((now() - lastScheduled) / 60000)
	String msg=sBLANK
	msg += "Checking daemon ${typ} | "
	msg += "Time since last ${timeSinceLast} mins -- lastScheduled == ${lastScheduled} | "

	if( timeSinceLastMins >= (intervalMins + getMinMinsBtwPolls() + 2)) result=false
	msg += !result ? "Not running | " : sBLANK
	return [res: result, msg: msg]
}

Boolean spawnDaemon(String daemon="all", Boolean unsched=true){
	String msgH="spawnDaemon(${daemon}, $unsched) | "
	String msg=sBLANK
	Boolean debugLevelFour=debugLevel(4)
	List<String> daemonList=["poll", "watchdog", "all"]

	daemon=daemon.toLowerCase()
	Boolean result=true

	if(daemon == "poll" || daemon == "all"){
		msg += " - Performing seance for daemon 'poll'"
		Integer pollingInterval=getPollingInterval()
		try {
			if(unsched){ unschedule(pollScheduled) }
			"runEvery${pollingInterval}Minute${pollingInterval!=1?'s':sBLANK}"(pollScheduled)
			state.lastScheduledPoll= now() - (getMinMinsBtwPolls()*60000L)
			state.lastScheduledPollDate=getTimestamp()
			//updateLastPoll(true)
			if(unsched){ // Only poll now if we were recovering - otherwise whoever called will handle the poll (as in initialize())
				if(debugLevelFour) LOG(msgH+msg+' calling pollScheduled', 4, sTRACE)
				pollScheduled('spawnDaemon')
			}
		} catch(Exception e){
			msg += " - Exception when performing spawn for ${daemon}."
			LOG(msgH+msg, 1, sERROR, e)
			result=false
			return result
		}
	}

	if(daemon == "watchdog" || daemon == "all"){
		msg += " - Performing seance for daemon 'watchdog'"
		try {
			if(unsched){ unschedule("scheduledWatchdog") }
			"runEvery${iWATCHDOGINTERVAL}Minutes"("scheduledWatchdog")
		} catch(Exception e){
			msg += " - Exception when performing spawn for ${daemon}."
			LOG(msgH+msg, 1, sERROR, e)
			result=false
			return result
		}
		state.lastScheduledWatchdog=now()
		state.lastScheduledWatchdogDate=getTimestamp()
		//forceNextPoll()
	}

	if(!daemonList.contains(daemon) ){
		msg += " - Unknown daemon: ${daemon} received. Do not know how to check this daemon."
		LOG(msgH+msg, 1, sERROR)
		result=false
		return result
	}
	msg += " - result is ${result}"
	if(result && debugLevelFour) LOG(msgH+msg, 4, sTRACE)
	if(!result) LOG(msgH+msg, 1, sWARN)
	return result
}


void updateLastPoll(Boolean isScheduled=false){
	if(isScheduled){
		state.lastScheduledPoll=now()
		state.lastScheduledPollDate=getTimestamp()
	}else{
		state.lastPoll=now()
		state.lastPollDate=getTimestamp()
	}
}

void poll(){
	LOG("poll()", 3, sTRACE)
	if(pollChildren()) updateLastPoll(false)
}

// Called by scheduled() event handler
void pollScheduled(String caller="runIn/Schedule"){
	LOG("pollScheduled(caller: $caller)", 3, sTRACE)
	if(pollChildren()) updateLastPoll(true)
}

void forceNextPoll(){
	Map updatesLog=state.updatesLog
	updatesLog.forcePoll=true
	state.updatesLog=updatesLog
}

// what child devices call on refresh()
void pollFromChild(String deviceId=sBLANK,Boolean force=false){
	LOG("pollFromChild()", 3, sTRACE)
	if(pollChildren(deviceId, force)) updateLastPoll(false)
}

Boolean pollChildren(String deviceId=sBLANK,Boolean force=false){
	Boolean result=false
	String msgH="pollChildren($deviceId, $force) | "
	// Prevent multiple concurrent poll cycles
	if((Boolean)state.inPollChildren){
		Long skipTime=(Long)state.skipTime ?: now()
		if((Long)state.skipTime != skipTime) state.skipTime=skipTime
		// Give the already running poll 20/25 seconds to complete
		if((now()-skipTime) < 25000L ){
			// Already/still polling, capture the arguments and skip this poll request
			if(force){
				forceNextPoll()
			}
			LOG(msgH+"prior poll not finished, skipping...")
			return result
		}
	}
	state.skipTime=null
	state.remove('skipTime')
	state.inPollChildren=true

	String version=getVersionLabel()
	LOG(msgH+"Checking for updates...",4,sTRACE)
	if(state.versionLabel != version){
		LOG(msgH+"Code updated: ${version} - re-initializing",2,sTRACE)
		state.versionLabel=version
		state.inPollChildren=false
		state.remove('inPollChildren')
		updated()
		return result
	}

	Boolean debugLevelFour=debugLevel(4)

	Long last = Math.max(((Long)state.lastPoll ?: 0L), ((Long)state.lastScheduledPoll?: 0L))
	Long aa=now() - last
	Boolean tooSoon=(aa < (getMinMinsBtwPolls()*60000L))
	if(tooSoon || debugLevelFour) {
		LOG(msgH+"=====> state.lastPoll(${state.lastPoll}) now(${now()}) state.lastPollDate(${state.lastPollDate})", 2, sTRACE)
		LOG(msgH+"=====> state.lastScheduledPoll(${state.lastScheduledPoll}) now(${now()}) state.lastScheduledPollDate(${state.lastScheduledPollDate})", 2, sTRACE)
	}
	if(tooSoon){
		LOG(msgH+"Too soon poll request, deferring...recent: ${aa/60000L} mins $last",2,sTRACE)
		state.inPollChildren=false
		state.remove('inPollChildren')
		runIn(85, poll, [overwrite: true]) // give time for command to complete; then get new status
		return result
	}

	// Start the new poll cycle
	state.pollAutoConnectAPIStart=now()
//	if(debugLevelFour) LOG(msgH, 1, sTRACE)
	Boolean forcePoll
	//String mowersToPoll

	Map updatesLog=state.updatesLog
	if(force || updatesLog.forcePoll){
		updatesLog.forcePoll=true
		forcePoll=true
		state.updatesLog=updatesLog
	} else {
		forcePoll=updatesLog.forcePoll
	}

	if(weAreLost(msgH, 'pollChildren')){
		state.inPollChildren=false
		state.remove('inPollChildren')
		return result
	}

	checkPolls(msgH)

	Map foo=getAutoMowers(forcePoll,"pollChildren")
	if(foo!=null) {
        //log.debug "in poll"
        
		updatesLog.forcePoll=false
		state.updatesLog=updatesLog
		if(((List<String>)settings.mowers)?.size() < 1){
			LOG(msgH+"Nothing to poll as there are no mowers currently selected", 1, sWARN)
			state.inPollChildren=false
			state.remove('inPollChildren')
			return result
		}
		String apiConnection=apiConnected()
		String slastPoll=(debugLevel(4)) ? "${apiConnection} @ ${state.lastScheduledPollDate}" : (apiConnection==sFULL) ? 'Succeeded' : (apiConnection==sWARN) ? 'Timed Out' : 'Failed'

		((List<String>)settings.mowers)?.each {String mower ->
			List<Map> flist=[]
			Map srcMap=getMowerMap(mower)
            
			if(srcMap) {
				Boolean moving=(String)srcMap.attributes.mower.activity in [ 'MOWING', 'GOING_HOME', 'LEAVING' ]
				Boolean onMain=(String)srcMap.attributes.mower.activity in [ 'CHARGING', 'PARKED_IN_CS' ]
				Boolean stuck=( (String)srcMap.attributes.mower.activity in [ 'STOPPED_IN_GARDEN' ] ||
					 (String)srcMap.attributes.mower.state in [ 'PAUSED', 'OFF', 'STOPPED', 'ERROR', 'FATAL_ERROR', 'ERROR_AT_POWER_UP' ] )
				Boolean parked=( (String)srcMap.attributes.mower.activity in [ 'PARKED_IN_CS', 'CHARGING' ])
				Boolean hold=( parked && (String)srcMap.attributes.mower.state in [ 'RESTRICTED' ])
				Boolean holdIndefinite=( hold && srcMap.attributes.planner.nextStartTimestamp == 0)
				Boolean holdUntilNext=( hold && srcMap.attributes.planner.nextStartTimestamp != 0)
				String dbg=settings.debugLevel == null ? "2" : settings.debugLevel
                
                
                //lgk get next run time into attribute  
                def nxt =  srcMap.attributes.planner.nextStartTimestamp
                def readableDate2 = new Date(nxt).format("E MMM dd, KK:mm a ", TimeZone.getTimeZone('UTC'))
                def now = new Date().format('MM/dd/yyyy h:mm a', location.timeZone)
                def errorString = "No Error"
                
                // also convert error time stamp to readable time.
                def ed = srcMap.attributes.mower.errorCodeTimestamp
                def readableErrorDate = new Date(ed).format('MM/dd/yyyy h:mm a', TimeZone.getTimeZone('UTC'))
                log.debug "error code time stamp = $ed rdate = $readableErrorDate"
                //if (ed == 0) readableErrorDate = "No Error"
                              
                // lgk do collisions here otherwise it comes out as null not zero
                def collisions = srcMap.attributes.statistics.numberofCollisions
                if (collisions == null) collisions = 0
                
                //lgk set readable error code if there is one and time
                
                log.info "Update!"
                // lgk cannot use unexpeccted error as in api as 0 as that is what is being returned here.
               // if (srcMap.attributes.mower.errorCode != null && srcMap.attributes.mower.errorCode != 0)
               // {
                    def errorCode = 16; //rcMap.attributes.mower.errorCode 
                    errorString = translateErrorCode(errorCode)                    
                    log.warn "Got an error code!, code = $errorCode  ($errorString) !"
                // }
                       
                 // lgk call fx in child direct to handle error cases so we can easily compare current and last error.
  			     def chld1=getChildDevice(mower)
                 if (chld1)
                    {
                        log.debug "calling handle errors with $errorString and $readableErrorDate"  
                        chld1.handleErrors(errorString, readableErrorDate)
                   }
                
				flist << ['name':	(String)srcMap.attributes.system.name ] //STRING
				flist << ['id':	srcMap.id ] //STRING
				flist << ['model':	srcMap.attributes.system.model ] //STRING
				flist << ['serialNumber':	srcMap.attributes.system.serialNumber.toString() ] //STRING
				flist << ['mowerStatus':	srcMap.attributes.mower.mode ] //MAIN_AREA, SECONDARY_AREA, HOME, DEMO, UNKNOWN
				flist << ['mowerActivity': srcMap.attributes.mower.activity] //UNKNOWN, NOT_APPLICABLE, MOWING, GOING_HOME, CHARGING, LEAVING, PARKED_IN_CS, STOPPED_IN_GARDEN
				flist << ['mowerState':	srcMap.attributes.mower.state] //UNKNOWN, NOT_APPLICABLE, PAUSED, IN_OPERATION, WAIT_UPDATING, WAIT_POWER_UP, RESTRICTED,
														// OFF, STOPPED, ERROR, FATAL_ERROR, ERROR_AT_POWER_UP
				flist << ['mowerConnected':	srcMap.attributes.metadata.connected] // TRUE or FALSE
				flist << ['mowerTimeStamp'	: srcMap.attributes.metadata.statusTimestamp] // LAST TIME connected (EPOCH LONG)
				flist << ['battery': srcMap.attributes.battery.batteryPercent] // Battery %
				flist << ['errorCode':	srcMap.attributes.mower.errorCode ] // integer
      
				flist << ['errorTimeStamp': srcMap.attributes.mower.errorCodeTimestamp] // (EPOCH LONG)
				flist << ['plannerNextStart': srcMap.attributes.planner.nextStartTimestamp] // (EPOCH LONG)
                flist << ['nextRun': readableDate2]
				flist << ['plannerOverride'	: srcMap.attributes.planner.override.action] // Override Action
                flist << ['lastUpdate'	: now] 

                // lgk get statistics
               
               // flist << ['cuttingBladeUsageTime' : srcMap.attributes.statistics.cuttingBladeUsageTime /3600]   lgk this is always 0 so not sure what it is i think it is supposed to sense last time you switched blades but doesnt work
                flist << ['numberOfChargingCycles' : srcMap.attributes.statistics.numberOfChargingCycles]
                flist << ['numberOfCollisions' : collisions]
                flist << ['totalChargingTime' : srcMap.attributes.statistics.totalChargingTime /3600]
                flist << ['totalCuttingTime' : srcMap.attributes.statistics.totalCuttingTime /3600]
                flist << ['totalRunningTime' : srcMap.attributes.statistics.totalRunningTime /3600]
                flist << ['totalSearchingTime' : srcMap.attributes.statistics.totalSearchingTime /3600]
               // flist << ['errorDesc' : errorString]
                flist << ['latitude' : srcMap.attributes?.positions?.getAt(0).latitude]
                flist << ['longitude' :srcMap.attributes?.positions?.getAt(0).longitude]
                 //flist << ['latitude' : 0]
                //flist << ['longitude' :0]
                
                //flist << ['lastErrorTime' : readableErrorDate]
                // end lgk attrs
                
				flist << ['motion': moving ? 'active' : 'inactive']
				flist << ['powerSource': onMain ? 'mains' : 'battery'] // "battery", "dc", "mains", "unknown"
				flist << ['stuck': stuck]
				flist << ['parked': parked]
				flist << ['hold': hold]
				flist << ['holdUntilNext': holdUntilNext]
				flist << ['holdIndefinite': holdIndefinite]

				flist << ['cuttingHeight': srcMap.attributes.settings.cuttingHeight] // Level
				flist << ['headlight': srcMap.attributes.settings.headlight.mode]

				flist << [apiConnected: apiConnection]
				flist << [lastPoll: slastPoll]
				flist << [debugLevel: dbg]

				def chld=getChildDevice(mower)
				if(chld) { chld.generateEvent(flist); result=true }
				else LOG(msgH+'Child device $mower not found', 1, sWARN)
			} else LOG(msgH+"no data from API for mower $mower", 3, sWARN)
		}
	} else { LOG(msgH+"no data",2,sWARN) }

	state.inPollChildren=false
	state.remove('inPollChildren')
	return result
}

// This only updates a few states
void generateEventLocalParams(){

	Boolean dbg2 = debugLevel(2)
	Boolean dbg4 = debugLevel(4)
	String apiConnection=apiConnected()
	String slastPoll= dbg4 ? "${apiConnection} @ ${state.lastScheduledPollDate}" : (apiConnection==sFULL ? 'Succeeded' : (apiConnection==sWARN ? 'Timed Out' : 'Failed'))
	String dbg= settings."debugLevel" == null ? "2" : settings."debugLevel"

	List<Map> data=[]
	data << [apiConnected: apiConnection]
	data << [lastPoll: slastPoll]
	data << ["debugLevel": dbg]

	String LOGtype= apiConnection==sLOST ? sERROR : (apiConnection==sWARN ? sWARN : sINFO)
	Integer lvl= apiConnection==sLOST ? 2 : (apiConnection==sWARN ? 2 : 4)
	Boolean a = lvl == 2 ? dbg2 : dbg4 // TODO THIS IS STRANGE INTELLIJ bug was debugLevel(lvl)
	if(a) {
		LOG("Updating API status with ${data}${LOGtype==sWARN ? " - will retry" : ''}", lvl, LOGtype)
	}

	// Iterate over all the children
	((List<String>)settings.mowers)?.each {
		getChildDevice(it)?.generateEvent(data)
	}
}

static String toQueryString(Map m){
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

void retryHelper() {
	Boolean a=refreshAuthToken('retryHelper')
}

// returns false if token is not valid
Boolean refreshAuthToken(String meth, child=null){
	String msgH="refreshAuthToken(${meth}, $child) | "
	String msg=sBLANK
	Boolean debugLevelFour=debugLevel(4)
//	if(debugLevelFour) LOG('Entered refreshAuthToken()', 4, sTRACE)

	Long texp = (Long)state.authTokenExpires
	String aT = atomicState.authToken
	Long timeBeforeExpiry= texp && aT ? texp - now() : 0L
	Boolean tokenStillGood=(timeBeforeExpiry > 2000L)
	msg += "Token is ${tokenStillGood ? "valid" : "invalid"} | texp: ${texp} | timeBeforeExpiry: ${timeBeforeExpiry} | authToken: ${aT} | "

	// check to see if token was recently refreshed
	Integer pollingIntrvMin=getPollingInterval()+2
	if(timeBeforeExpiry > (pollingIntrvMin*60000L)){
		msg += "exiting, token expires in ${timeBeforeExpiry/1000} seconds"
		if(debugLevelFour) LOG(msgH+msg,4,sINFO)
		// Double check that the daemons are still running
		checkPolls(msgH)
		return tokenStillGood
	}

	msg += "Want to refresh token | "
	def rt = atomicState.refreshToken
	if(!rt || timeBeforeExpiry < 30L) {
		state.authToken=sNULL
		tokenStillGood=false
		if(msg) { LOG(msgH + msg, 2, sTRACE); msg=sBLANK }
		apiLost(msgH+"No refresh Token (${rt}) or expired refresh token ${timeBeforeExpiry} ${texp} | CLEARED authToken due to no refreshToken or expired authToken")
	}else{
		msg +='Performing token refresh'
		Map rdata=[grant_type: 'refresh_token', client_id: getHusqvarnaApiKey(), refresh_token: "${rt}"]
		String data=rdata.collect{ String k,v -> encodeURIComponent(k)+'='+encodeURIComponent(v) }.join('&')
		Map refreshParams=[
			uri: getApiEndpoint()+"/token",
			query: null,
			contentType: "application/x-www-form-urlencoded",
			body: data,
			timeout: 30
		]

		if(debugLevelFour){
			msg +="refreshParams=${refreshParams} "
			msg += "OAUTH Token=state: ${aT} "
			msg += "Refresh Token=state: ${rt}  "
		}

		msg += "state.authTokenExpires=${texp}  ${formatDt(new Date(texp))} "
		if(msg) {
			LOG(msgH + msg, 2, sTRACE) // 4
			msg=sBLANK
		}
		try {
			httpPost(refreshParams){ resp ->
				//if(debugLevelFour) LOG("Inside httpPost resp handling.", 1, sTRACE, null, child)
				if(resp && resp.isSuccess() && resp.status && (resp.status == 200)){
					if(debugLevelFour) LOG(msgH+'200 Response received - Extracting info.', 4, sTRACE, null, child ) // 4

//					parseAuthResponse(resp)
					String kk
					resp.data.each { kk=it.key }
					Map ndata=(Map)new JsonSlurper().parseText(kk)
//					log.debug "ndata : ${ndata}"

					String oldAuthToken=aT
					if(oldAuthToken == ndata.access_token){
						LOG(msgH+'WARN: state.authToken did NOT update properly! This is likely a transient problem', 1, sWARN, null, child)
						return tokenStillGood
					}

					if(state.reAttempt) { state.reAttempt=0; state.remove('reAttempt') }
					if(state.inTimeoutRetry){ state.inTimeoutRetry=0; state.remove('inTimeoutRetry') }
					state.lastTokenRefresh=now()
					state.lastTokenRefreshDate=getTimestamp()

					Long tt=(Long)now() + (ndata.expires_in * 1000)
					if(debugLevelFour){ // 4
						msg += "Updated state.authTokenExpires=${tt} "
						msg += "Refresh Token=state: ${rt} == in: ${ndata.refresh_token} "
						msg += "OAUTH Token=state: ${aT} == in: ${ndata.access_token}"
						LOG(msgH+msg, 4, sTRACE, null, child)
					}
					state.authTokenExpires=tt
					state.refreshToken=ndata.refresh_token
					state.authToken=ndata.access_token
					//atomicState.authTokenExpires=tt
					atomicState.refreshToken=ndata.refresh_token
					atomicState.authToken=ndata.access_token
					tokenStillGood=true

					LOG("refreshAuthToken() - Success! Token expires in ${String.format("%.2f",ndata.expires_in/60)} minutes", 3, sINFO, null, child) // 3

					// Tell the children that we are once again connected to the AutoConnect API Cloud
					if(apiConnected() != sFULL){ apiRestored(false) }

					checkPolls(msgH)
				}else{
					LOG(msgH+"Failed ${resp.status} : ${resp.status.code}!", 1, sERROR, null, child)
				}
			}
		} catch(e){
			Integer maxAttempt=5
			if("${e}".contains("HttpResponseException")){
				LOG(msgH+"HttpResponseException occurred. StatusCode: ${e.statusCode}", 1, sERROR, e, child)
			} else if("${e}".contains("TimeoutException")){
				maxAttempt= 20
				// Likely bad luck and network overload, move on and let it try again
				LOG(msgH+"TimeoutException", 1, sWARN, e, child)
			} else {
				LOG(msgH + "Not Sure of issue", 1, sERROR, e, child)
			}
			Integer attempts = (Integer)state.reAttempt
			attempts= attempts!=null ? attempts+1 : 1
			state.reAttempt=attempts
			if(attempts > maxAttempt || timeBeforeExpiry < 1L){
				state.authToken=sNULL
				tokenStillGood=false
				apiLost(msgH+"CLEARING AUTH TOKEN - Too many retries (${state.reAttempt - 1}) for token refresh, or expired auth token ${timeBeforeExpiry} ${state.authTokenExpires}")
			}else{
				LOG(msgH+"Setting up runIn for refreshAuthToken", 2, sTRACE, null, child) // 4
				Integer retryFactor = attempts > 12 ? 12 : attempts
				runIn(iREATTEMPTINTERVAL*retryFactor, retryHelper, [overwrite: true])
				if(attempts > 3 && apiConnected() == sFULL){
					state.connected=sWARN
					updateMyLabel()
					generateEventLocalParams()
				}
			}
		}
	}

	return tokenStillGood
}

//String getServerUrl()			{ return getFullApiServerUrl() }
//String getShardUrl()			{ return getFullApiServerUrl()+"?access_token=${state.accessToken}" }

void LOG(String message, Integer level=3, String logType=sDEBUG, ex=null, child=null, Boolean event=false, Boolean displayEvent=true){
	if(logType == sNULL) logType=sDEBUG

	if(logType == sERROR){
		String a=getTimestamp()
		state.lastLOGerror="${message} @ "+a
		state.LastLOGerrorDate=a
	}else{
		Integer dbgLevel=getIDebugLevel()
		if(level > dbgLevel) return
	}

	if(!lLOGTYPES.contains(logType)){
		logerror("LOG() - Received logType (${logType}) which is not in the list of allowed types ${lLOGTYPES}, message: ${message}, level: ${level}")
		if(event && child){ debugEventFromParent(child, "LOG() - Invalid logType ${logType} (warn)") }
		logType=sDEBUG
	}

	String prefix=sBLANK
	if( dbgLevel == 5 ){ prefix='LOG: ' }
	"log${logType}"("${prefix}${message}", ex)
	if(event){ debugEvent(message, displayEvent) }
	if(child){ debugEventFromParent(child, message+" (${logType})") }
}

private void logdebug(String msg, Exception ex=null){ log.debug logPrefix(msg, "purple") }
private void loginfo(String msg, Exception ex=null){ log.info sSPACE + logPrefix(msg, "#0299b1") }
private void logtrace(String msg, Exception ex=null){ log.trace logPrefix(msg, sCLRGRY) }
private void logwarn(String msg, Exception ex=null){ logexception(msg,ex,sWARN, sCLRORG) }
void logerror(String msg, Exception ex=null){ logexception(msg,ex,sERROR, sCLRRED) }

void logexception(String msg, Exception ex=null, String typ, String clr) {
	String msg1=ex ? " Exception: ${ex}" : sBLANK
	log."$typ" logPrefix(msg+msg1, clr)
	String a
	try {
		if(ex) a=getExceptionMessageWithLine(ex)
	} catch(ignored){ }
	if(a) log."$typ" logPrefix(a, clr)
}

static String logPrefix(String msg, String color=sNULL){
	return span("AutoMower App (v" + getVersionNum() + ") | ", sCLRGRY) + span(msg, color)
}

static String span(String str, String clr=sNULL, String sz=sNULL, Boolean bld=false, Boolean br=false){ return str ? "<span ${(clr || sz || bld) ? "style='${clr ? "color: ${clr};" : sBLANK}${sz ? "font-size: ${sz};" : sBLANK}${bld ? "font-weight: bold;" : sBLANK}'" : sBLANK}>${str}</span>${br ? sLINEBR : sBLANK}" : sBLANK }

Integer getIDebugLevel(){
	return (settings.debugLevel ?: 3) as Integer
}

Boolean debugLevel(Integer level=3){
	return (getIDebugLevel() >= level)
}

void debugEvent(String message, Boolean displayEvent=false){
	Map results=[
		name: 'appdebug',
		descriptionText: message,
		displayed: displayEvent
	]
	if( debugLevel(4) ){ LOG("Generating AppDebug Event: ${results}", 3, sDEBUG) }
	sendEvent(results)
}

@SuppressWarnings('GrMethodMayBeStatic')
void debugEventFromParent(child, String message){
	if(child){ child.generateEvent([ [debugEventFromParent: message] ]) }
}

Boolean notifyNowOK(){
	Boolean modeOK=(List)settings.speakModes ? ((List)settings.speakModes && ((List)settings.speakModes).contains((String)location.mode)) : true
	Boolean timeOK=settings.speakTimeStart? myTimeOfDayIsBetween((Date)timeToday(settings.speakTimeStart), (Date)timeToday(settings.speakTimeEnd), new Date()) : true
	return (modeOK && timeOK)
}

private static Boolean myTimeOfDayIsBetween(Date fromDate, Date toDate, Date checkDate)	{
	if(toDate == fromDate){
		return false	// blocks the whole day
	} else if(toDate < fromDate){
		if(checkDate.before(fromDate)){
			fromDate -= 1
		}else{
			toDate += 1
		}
	}
	return (!checkDate.before(fromDate) && !checkDate.after(toDate))
}

// send both push notification and mobile activity feeds
void sendMessage(String notificationMessage){
	String msgH="sendMessage() | "
	// notification is sent to remind user no more than once per hour
	Long otsp=state.timeSendPush ?: null
	Boolean sendNotification=!(otsp && ((now() - otsp) < 3600000))

	String msg1=sendNotification ? "Sending" : "Not sending"
	msg1+=" Notification Message: ${notificationMessage}"
	msg1+=" Last Notification Time: ${state.timeSendPush}"
	LOG(msgH+msg1, 2, sTRACE)

	if(sendNotification){
		Long ntsp=otsp
		String msgPrefix=state.appDisplayName + " at ${location.name}: "
		String msg=msgPrefix + notificationMessage
		Boolean addFrom=true // (msgPrefix && !msgPrefix.startsWith("From "))
		if(settings.notifiers){
			if( sendNotifications(msgPrefix, notificationMessage) ){
				ntsp=now()
			}
		}
		if((Boolean)settings.speak){
			if(notifyNowOK()){
				if(settings.speechDevices != null){
					settings.speechDevices.each {
						it.speak((addFrom?"From ":sBLANK) + msg )
					}
					ntsp=now()
				}
				if(settings.musicDevices != null){
					settings.musicDevices.each {
						it.setLevel( settings.volume )
						it.playText((addFrom?"From ":sBLANK) + msg )
					}
					ntsp=now()
				}
			} else LOG(msgH+"speak/music notification restricted", 2, sTRACE)
		}
		if(otsp != ntsp) {
			state.timeSendPush=ntsp
		} else LOG(msgH+"settings did not have any message sent", 2, sTRACE)
	}
}

Boolean sendNotifications( String msgPrefix, String msg ){
	if(!settings.notifiers){
		LOG("sendNotifications(): no notifiers!",2,sWARN)
		return false
	}

	List echo=((List)settings.notifiers).findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
	List notEcho=echo ? (List)settings.notifiers - echo : (List)settings.notifiers
	List echoDeviceObjs=[]
	if((Boolean)settings.echoAnnouncements){
		if(echo?.size()){
			// Get all the Echo Speaks devices to speak at once
			echo.each {
				String deviceType=it.currentValue('deviceType') as String
				// deviceSerial is an attribute as of Echo Speaks device version 3.6.2.0
				String deviceSerial=(it.currentValue('deviceSerial') ?: it.deviceNetworkId.toString().split(/\|/).last()) as String
				echoDeviceObjs.push([deviceTypeId: deviceType, deviceSerialNumber: deviceSerial])
			}
			if(echoDeviceObjs?.size()){
				//NOTE: Only sends command to first device in the list | We send the list of devices to announce one and then Amazon does all the processing
				//def devJson=new groovy.json.JsonOutput().toJson(echoDeviceObjs)
				echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:state.appDisplayName), echoDeviceObjs)	// , changeVol, restoreVol) }
			}
			// The rest get a standard deviceNotification
			if(notEcho.size()) notEcho*.deviceNotification(msg)
		}else{
			// No Echo Speaks devices
			settings.notifiers*.deviceNotification(msg)
		}
	}else{
		// Echo Announcements not enabled - just do deviceNotifications, but only if Do Not Disturb is not on
		if(echo?.size()) echo*.deviceNotification(msg)
		if(notEcho.size()) notEcho*.deviceNotification(msg)
	}
	return true
}

/*
void sendActivityFeeds(String notificationMessage){
	def devices=getChildDevices()
	devices.each { child ->
		child.generateActivityFeedsEvent(notificationMessage) //parse received message from parent
	}
} */
/*
static String toJson(Map m){
	return JsonOutput.toJson(m)
} */

Integer getPollingInterval(){
	return ((settings?.pollingInterval!= null) ? settings.pollingInterval : 15) as Integer
}

static String getTimestamp(){
	return new Date().format("yyyy-MM-dd HH:mm:ss z")
}

String apiConnected(){
	// values can be sFULL, sWARN, sLOST
	if(!(String)state.connected){ state.connected=sWARN; updateMyLabel(); return sWARN }else{ return (String)state.connected }
}

void apiRestored(Boolean chkP=true){
	state.connected=sFULL
	updateMyLabel()
	unschedule("notifyApiLostHelper")
	unschedule("notifyApiLost")
	state.lastScheduledPoll=null
	if(chkP) checkPolls('apiRestored() ', true)
	generateEventLocalParams() // Update the connection status
}

Map getDebugDump(){
	Map debugParams=[when:"${getTimestamp()}", whenEpoch:"${now()}",
				lastPollDate:"${state.lastScheduledPollDate}", lastScheduledPollDate:"${state.lastScheduledPollDate}",
				lastScheduledWatchdogDate:"${state.lastScheduledWatchdogDate}",
				lastTokenRefreshDate:"${state.lastTokenRefreshDate}",
				initializedEpoch:"${state.initializedEpoch}", initializedDate:"${state.initializedDate}",
				lastLOGerror:"${state.lastLOGerror}", authTokenExpires:"${(Long)state.authTokenExpires}"
			]
	return debugParams
}

void apiLost(String where="[where not specified]"){
	if(apiConnected() == sLOST){
		LOG("apiLost($where) - already in lost state.", 5, sTRACE)
	}else{
		LOG("apiLost() - ${where}: Lost connection with API.", 1, sERROR)
		state.apiLostDump=getDebugDump()
		state.connected=sLOST
		updateMyLabel()
	}
	runIn(15, notifyApiLostHelper, [overwrite: true])
}

void notifyApiLostHelper() {
	if( (String)state.connected == sLOST ){
		LOG("Unscheduling Polling and refreshAuthToken. User MUST reintialize the connection with AutoConnect by running the AutoMower Manager App and logging in again", 0, sERROR)
		generateEventLocalParams() // Update the connection status
		// put a log for each child that we are lost
		if( debugLevel(3) ){
			def d=getChildDevices()
			d?.each { oneChild ->
				LOG("apiLost() - notifying child: ${oneChild.device.displayName} of loss", 3, sERROR, null, oneChild)
			}
		}
		unschedule(pollScheduled)
		unschedule(scheduledWatchdog)
		runEvery3Hours(notifyApiLost)
	}
	notifyApiLost()
}

void notifyApiLost(){
	if( (String)state.connected == sLOST ){
		generateEventLocalParams()
		String notificationMessage="Your AutoMower Manager mower${((List<String>)settings.mowers)?.size()>1?'s are':' is'} disconnected AutoConnect. Please go to the AutoMower Manager and re-enter your AutoConnect account login credentials."
		sendMessage(notificationMessage)
		LOG("notifyApiLost() - API Connection Previously Lost. User MUST reintialize the connection with AutoConnect by running the AutoMower Manager App and logging in again", 0, sERROR)
	}else{
		// Must have restored connection
		unschedule("notifyApiLostHelper")
		unschedule("notifyApiLost")
	}
}

void runEvery6Minutes(handler){
	Integer randomSeconds=randomSeed.nextInt(59)
	schedule("${randomSeconds} 0/6 * * * ?", handler)
}

void runEvery60Minutes(handler){
	runEvery1Hour(handler)
}

void updateMyLabel(){
	// Display connection status as part of the label...
	String myLabel=(String)state.appDisplayName
	if((myLabel == null) || !app.label.startsWith(myLabel)){
		myLabel=app.label
		if(!myLabel.contains('<span')) state.appDisplayName=myLabel
	}
	if(myLabel.contains('<span')){
		myLabel=myLabel.substring(0, myLabel.indexOf('<span'))
		state.appDisplayName=myLabel
	}
	String newLabel
	String key=(String)state.connected
	switch (key){
		case sFULL:
			newLabel=myLabel + "<span style=\"color:green\"> Online</span>"
			break
		case sWARN:
			newLabel=myLabel + "<span style=\"color:orange\"> Warning</span>"
			break
		case sLOST:
			newLabel=myLabel + "<span style=\"color:red\"> Offline</span>"
			break
		default:
			newLabel=myLabel
			break
	}
	if(newLabel && ((String)app.label != newLabel)) app.updateLabel(newLabel)
}

static String theMower()				{ return '<img src=https://raw.githubusercontent.com/imnotbob/autoMower/master/images/automower310d.jpg width=78 height=78 align=right></img>'}
static String getTheSectionMowerLogo()		{ return '<img src=https://raw.githubusercontent.com/imnotbob/autoMower/master/images/automower310d.jpg width=25 height=25 align=left></img>'}

static String pageTitle		(String txt)	{ return getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') }
//static String pageTitleOld	(String txt)	{ return getFormat('header-ecobee','<h2>'+txt+'</h2>') }
static String sectionTitle	(String txt)	{ return getTheSectionMowerLogo() + getFormat('header-nobee','<h3><b>&nbsp;&nbsp;'+txt+'</b></h3>') }
static String smallerTitle	(String txt)	{ return txt ? '<h3 style="color:#5BBD76"><b><u>'+txt+'</u></b></h3>' : sBLANK}
//static String sampleTitle	(String txt)	{ return '<b><i>'+txt+'<i></b>' }
static String inputTitle	(String txt)	{ return '<b>'+txt+'</b>' }
//static String getWarningText()				{ return "<span style='color:red'><b>WARNING: </b></span>"}

static String getFormat(String type, String myText=sBLANK){
	switch(type){
		case "header-ecobee":
			return "<div style='color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${theMower()}${myText}</div>"
			break
		case "header-nobee":
			return "<div style='width:50%;min-width:400px;color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;padding-right:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${myText}</div>"
			break
		case "line":
			return "<hr style='background-color:#5BBD76; height: 1px; border: 0;'></hr>"
			break
		case "title":
			return "<h2 style='color:#5BBD76;font-weight: bold'>${myText}</h2>"
			break
		case "warning":
			return "<span style='color:red'><b>WARNING: </b><i></span>${myText}</i>"
			break
		case "note":
			return "<b>NOTE: </b>${myText}"
			break
		default:
			return myText
			break
	}
}

String getDtNow() {
	Date now=new Date()
	return formatDt(now)
}

String formatDt(Date dt, Boolean tzChg=true) {
	SimpleDateFormat tf=new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
	if(tzChg) { if(location.timeZone) { tf.setTimeZone((TimeZone)location?.timeZone) } }
	return (String)tf.format(dt)
}

@Field static final List<String> svdTSValsFLD=["lastCookieRrshDt"]
@Field volatile static Map<String,Map> tsDtMapFLD=[:]

private void updTsVal(String key, String dt=sNULL) {
	String val=dt ?: getDtNow()
	if(key in svdTSValsFLD) { updServerItem(key, val); return }

	String appId=app.getId().toString()
	Map data=tsDtMapFLD[appId] ?: [:]
	if(key) data[key]=val
	tsDtMapFLD[appId]=data
	tsDtMapFLD=tsDtMapFLD
}

private void remTsVal(key) {
	String appId=app.getId().toString()
	Map data=tsDtMapFLD[appId] ?: [:]
	if(key) {
		if(key instanceof List) {
			List<String> aa=(List<String>)key
			aa.each { String k->
				if(data.containsKey(k)) { data.remove(k) }
				if(k in svdTSValsFLD) { remServerItem(k) }
			}
		}else{
			String sKey=(String)key
			if(data.containsKey(sKey)) { data.remove(sKey) }
			if(sKey in svdTSValsFLD) { remServerItem(sKey) }
		}
		tsDtMapFLD[appId]=data
		tsDtMapFLD=tsDtMapFLD
	}
}

private String getTsVal(String key) {
	if(key in svdTSValsFLD) {
		return (String)getServerItem(key)
	}
	String appId=app.getId().toString()
	Map tsMap=tsDtMapFLD[appId]
	if(key && tsMap && tsMap[key]) { return (String)tsMap[key] }
	return sNULL
}

Integer getLastTsValSecs(String val, Integer nullVal=1000000) {
	return (val && getTsVal(val)) ? GetTimeDiffSeconds(getTsVal(val)).toInteger() : nullVal
}

@Field volatile static Map<String,Map> serverDataMapFLD=[:]

void updServerItem(String key, val) {
	Map data=atomicState?.serverDataMap
	data=data ?: [:]
	if(key) {
		String appId=app.getId().toString()
		data[key]=val
		atomicState.serverDataMap=data
		serverDataMapFLD[appId]= [:]
		serverDataMapFLD=serverDataMapFLD
	}
}

void remServerItem(key) {
	Map data=atomicState?.serverDataMap
	data=data ?: [:]
	if(key) {
		if(key instanceof List) {
			List<String> aa=(List<String>)key
			aa?.each { String k-> if(data.containsKey(k)) { data.remove(k) } }
		}else{ if(data.containsKey((String)key)) { data.remove((String)key) } }
		String appId=app.getId().toString()
		atomicState?.serverDataMap=data
		serverDataMapFLD[appId]= [:]
		serverDataMapFLD=serverDataMapFLD
	}
}

def getServerItem(String key) {
	String appId=app.getId().toString()
	Map fdata=serverDataMapFLD[appId]
	if(fdata == null) fdata=[:]
	if(key) {
		if(fdata[key] == null) {
			Map sMap=atomicState?.serverDataMap
			if(sMap && sMap[key]) {
				fdata[key]=sMap[key]
			}
		}
		return fdata[key]
	}
	return null
}

Long GetTimeDiffSeconds(String lastDate, String sender=sNULL) {
	try {
		if(lastDate?.contains("dtNow")) { return 10000 }
		Date lastDt=Date.parse("E MMM dd HH:mm:ss z yyyy", lastDate)
		Long start=lastDt.getTime()
		Long stop=now()
		Long diff= (Long)((stop - start) / 1000L)
		return diff.abs()
	} catch(ex) {
		String sndr = sender ? "$sender | " : sSPACE
		LOG("GetTimeDiffSeconds (${sndr}lastDate: ${lastDate})", 1, sERROR, ex)
		return 10000L
	}
}


def translateErrorCode(Number theErrorCode)
{
    
    def errorString = "No Error"
  
switch (theErrorCode)
{
case     0 :	errorString = "Unexpected error"
                break
case     1 :	errorString = "Outside working area"
                break
case     2 :	errorString = "No loop signal"
                break
case     3 :	errorString = "Wrong loop signal"
                break
case     4 :	errorString = "Loop sensor problem, front"
                break
case     5 :	errorString = "Loop sensor problem, rear"
                break
case     6 :	errorString = "Loop sensor problem, left"
                break
case     7 :	errorString = "Loop sensor problem, right"
                break
case     8 :	errorString = "Wrong PIN code"
                break
case     9 :	errorString = "Trapped"
                break
case     10 :	errorString = "Upside down"
                break
case     11 :	errorString = "Low battery"
                break
case     12 :	errorString = "Empty battery"
                break
case     13 :	errorString = "No drive"
                break
case     14 :	errorString = "Mower lifted"
                break
case     15 :	errorString = "Lifted"
                break
case     16 :	errorString = "Stuck in charging station"
                break
case     17 :	errorString = "Charging station blocked"
                break
case     18 :	errorString = "Collision sensor problem, rear"
                break
case     19 :	errorString = "Collision sensor problem, front"
                break
case     20 :	errorString = "Wheel motor blocked, right"
                break
case     21 :	errorString = "Wheel motor blocked, left"
                break
case     22 :	errorString = "Wheel drive problem, right"
                break
case     23 :	errorString = "Wheel drive problem, left"
                break
case     24 :	errorString = "Cutting system blocked"
                break
case     25 :	errorString = "Cutting system blocked"
                break
case     26 :	errorString = "Invalid sub-device combination"
                break
case     27 :	errorString = "Settings restored"
                break
case     28 :	errorString = "Memory circuit problem"
                break
case     29 :	errorString = "Slope too steep"
                break
case     30 :	errorString = "Charging system problem"
                break
case     31 :	errorString = "STOP button problem"
                break
case     32 :	errorString = "Tilt sensor problem"
                break
case     33 :	errorString = "Mower tilted"
                break
case     34 :	errorString = "Cutting stopped - slope too steep"
                break
case     35 :	errorString = "Wheel motor overloaded, right"
                break
case     36 :	errorString = "Wheel motor overloaded, left"
                break
case     37 :	errorString = "Charging current too high"
                break
case     38 :	errorString = "Electronic problem"
                break
case     39 :	errorString = "Cutting motor problem"
                break
case     40 :	errorString = "Limited cutting height range"
                break
case     41 :	errorString = "Unexpected cutting height adj"
                break
case     42 :	errorString = "Limited cutting height range"
                break
case     43 :	errorString = "Cutting height problem, drive"
                break
case     44 :	errorString = "Cutting height problem, curr"
                break
case     45 :	errorString = "Cutting height problem, dir"
                break
case     46 :	errorString = "Cutting height blocked"
                break
case     47 :	errorString = "Cutting height problem"
                break
case     48 :	errorString = "No response from charger"
                break
case     49 :	errorString = "Ultrasonic problem"
                break
case     50 :	errorString = "Guide 1 not found"
                break
case     51 :	errorString = "Guide 2 not found"
                break
case     52 :	errorString = "Guide 3 not found"
                break
case     53 :	errorString = "GPS navigation problem"
                break
case     54 :	errorString = "Weak GPS signal"
                break
case     55 :	errorString = "Difficult finding home"
                break
case     56 :	errorString = "Guide calibration accomplished"
                break
case     57 :	errorString = "Guide calibration failed"
                break
case     58 :	errorString = "Temporary battery problem"
                break
case     59 :	errorString = "Temporary battery problem"
                break
case     60 :	errorString = "Temporary battery problem"
                break
case     61 :	errorString = "Temporary battery problem"
                break
case     62 :	errorString = "Temporary battery problem"
                break
case     63 :	errorString = "Temporary battery problem"
                break
case     64 :	errorString = "Temporary battery problem"
                break
case     65 :	errorString = "Temporary battery problem"
                break
case     66 :	errorString = "Battery problem"
                break
case     67 :	errorString = "Battery problem"
                break
case     68 :	errorString = "Temporary battery problem"
                break
case     69 :	errorString = "Alarm! Mower switched off"
                break
case     70 :	errorString = "Alarm! Mower stopped"
                break
case     71 :	errorString = "Alarm! Mower lifted"
                break
case     72 :	errorString = "Alarm! Mower tilted"
                break
case     73 :	errorString = "Alarm! Mower in motion"
                break
case     74 :	errorString = "Alarm! Outside geofence"
                break
case     75 :	errorString = "Connection changed"
                break
case     76 :	errorString = "Connection NOT changed"
                break
case     77 :	errorString = "Com board not available"
                break
case     78 :	errorString = "Slipped - Mower has Slipped.Situation not solved with moving pattern"
                break
case     79 :	errorString = "Invalid battery combination - Invalid combination of different battery types."
                break
case     80 :	errorString = "Cutting system imbalance :	errorString = 'Warning'"
                break
case     81 :	errorString = "Safety function faulty"
                break
case     82 :	errorString = "Wheel motor blocked, rear right"
                break
case     83 :	errorString = "Wheel motor blocked, rear left"
                break
case     84 :	errorString = "Wheel drive problem, rear right"
                break
case     85 :	errorString = "Wheel drive problem, rear left"
                break
case     86 :	errorString = "Wheel motor overloaded, rear right"
                break
case     87 :	errorString = "Wheel motor overloaded, rear left"
                break
case     88 :	errorString = "Angular sensor problem"
                break
case     89 :	errorString = "Invalid system configuration"
                break
case     90 :	errorString = "No power in charging station"
                break
case     91 :	errorString = "Switch cord problem"
                break
case     92 :	errorString = "Work area not valid"
                break
case     93 :	errorString = "No accurate position from satellites"
                break
case     94 :	errorString = "Reference station communication problem"
                break
case     95 :	errorString = "Folding sensor activated"
                break
case     96 :	errorString = "Right brush motor overloaded"
                break
case     97 :	errorString = "Left brush motor overloaded"
                break
case     98 :	errorString = "Ultrasonic Sensor 1 defect"
                break
case     99 :	errorString = "Ultrasonic Sensor 2 defect"
                break
case     100 :	errorString = "Ultrasonic Sensor 3 defect"
                break
case     101 :	errorString = "Ultrasonic Sensor 4 defect"
                break
case     102 :	errorString = "Cutting drive motor 1 defect"
                break
case     103 :	errorString = "Cutting drive motor 2 defect"
                break
case     104 :	errorString = "Cutting drive motor 3 defect"
                break
case     105 :	errorString = "Lift Sensor defect"
                break
case     106 :	errorString = "Collision sensor defect"
                break
case     107 :	errorString = "Docking sensor defect"
                break
case     108 :	errorString = "Folding cutting deck sensor defect"
                break
case     109 :	errorString = "Loop sensor defect"
                break
case     110 :	errorString = "Collision sensor error"
                break
case     111 :	errorString = "No confirmed position"
                break
case     112 :	errorString = "Cutting system major imbalance"
                break
case     113 :	errorString = "Complex working area"
                break
case     114 :	errorString = "Too high discharge current"
                break
case     115 :	errorString = "Too high internal current"
                break
case     116 :	errorString = "High charging power loss"
                break
case     117 :	errorString = "High internal power loss"
                break
case     118 :	errorString = "Charging system problem"
                break
case     119 :	errorString = "Zone generator problem"
                break
case     120 :	errorString = "Internal voltage error"
                break
case     121 :	errorString = "High internal temerature"
                break
case     122 :	errorString = "CAN error"
                break
case     123 :	errorString = "Destination not reachable"
                break
default: errorString = "No Error"
    
}    
    return errorString
}


    

@Field static final List<String> lLOGTYPES =	['error', 'debug', 'info', 'trace', 'warn']

@Field static final String sDEBUG		= 'debug'
@Field static final String sERROR		= 'error'
@Field static final String sINFO		= 'info'
@Field static final String sTRACE		= 'trace'
@Field static final String sWARN		= 'warn'


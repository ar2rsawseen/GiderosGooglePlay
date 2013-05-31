<h1>Game services installation instructions</h1>

<h4>1) Prerequesites</h4>
<ul>
<li>Install Google Play Services: http://developer.android.com/google/play-services/setup.html<li>
<li>Set up Google Play Services for your app: https://developers.google.com/games/services/android/quickstart</li>
</ul>
<p>Things to note:
<ul>
<li>You appID, which you will be using (don't mix with ClientID, you don't need it for Androids)</li>
<li>When entering the SHA1 fingerprint for your app authorization, don't use the default one, you need to either:
<ul>
<li>Genereate your own for production key from your production certificate, but it means you will only be able to use it after you export and sign app, you won't be able to debug with it</li>
<li>Use android debug key, which you can get in Eclipse, by choosing Window->Preferences->Android->Build, then you will be able to debug your app with Google Play Service</li>
<ul></li>
<li>Changing fingerprint from debug to production is a pain, better set up two diferent apps to do that.</li>
<li>Add email of your google account used on phone for Test users, so you qill be able to test Google Services without publishing the app.</li>
</ul></p>

<h4>2) Gideros project</h4>
<ul>
<li>Create Gideros project</li>
<li>Export it as Android project</li>
<li>Import it in Eclipse</li>
</ul>

<h4>3) Copying files</h4>
<ul>
<li>Copy .so files from libs folder to each separate armeabi folder</li>
<li>Copy from src/giderosmobile/android/plugins folder named "googleplaygame" into your project's src/giderosmobile/android/plugins folder</li>
<li>Copy ids.xml from res/values to res/values in your project</li>
</ul>

<h4>4) Modify Project</h4>
<ul>
<li>Reference in Eclipse imported Google Play Service by making a right click on the project, select Properties, then Android and push Add button in a Lirary section and select google-play-services-lib (if it is not there, you haven't properly imported in Eclipse, see Prerequesites, Install Google Play Services) </li>
<li>Add your appID to ids.xml inside res/values folder</li>
<li>Add &lt;meta-data android:name="com.google.android.gms.games.APP_ID" android:value="@string/app_id" /&gt; to AndroidManifest file inside application tag</li>
<li>Load plugin library in main Activity: System.loadLibrary("gms");</li>
<li>Add external class: "com.giderosmobile.android.plugins.googleplaygame.GGooglePlay"</li>
</ul>



<h1>Plugin Reference API</h1>
<h2>Methods</h2>
<ul>
<li>googleplay:login() --login</li>
<li>googleplay:logout() --logout</li>
<li>googleplay:showSettings() --show settings screen</li>
<li>googleplay:showLeaderboard(leaderboardId) --show leaderboard screen</li>
<li>googleplay:reportScore(leaderboardId, score) --post score to service</li>
<li>googleplay:showAchievements() --show achievements screen</li>
<li>googleplay:reportAchievement(achId [, numSteps]) --achievement unlocked or progressed by specified amount of steps of total defined steps</li>
<li>googleplay:loadAchievements() --retrieve all defined achievemenets</li>
<li>googleplay:loadScores(leaderboardId [,timespan, participants, maxResults]) --retrieve all scores for specific leaderboard ina specific timespan for specific participans</li>
<li>googleplay:autoMatch(minPlayers, maxPlayers) --create a quick game between randomly selected players</li>
<li>googleplay:invitePlayers(minPlayers, maxPlayers) --allow user to invite players to a game</li>
<li>googleplay:joinRoom(invitationId) --joins the game you've been invited to</li>
<li>googleplay:showInvitations() --show screen for managing invitations</li>
<li>googleplay:showWaitingRoom(minPlayers) --show waiting room before the game with the list of players</li>
<li>googleplay:sendTo(playerId, data, isReliable) --send data to specific user by id</li>
<li>googleplay:sendToAll(data, isReliable) -- send to all users</li>
<li>googleplay:getCurrentPlayer() --get your current user name</li>
<li>googleplay:getAllPlayers() --retrieve information (id and name) for all users involved in game</li>
</ul>

<h2>Properties</h2>
<ul>
<li>GooglePlay.UNLOCKED --achievement state unlocked</li>
<li>GooglePlay.REVEALED --achievement state available to user</li>
<li>GooglePlay.HIDDEN   --achievement state unknown to user </li>
</ul>
<ul>
<li>GooglePlay.ALL_TIME --leaderboard timespan all time</li>
<li>GooglePlay.WEEK     --leaderboard timespan week</li>
<li>GooglePlay.TODAY    --leaderboard timespan today</li>
</ul>
<ul>
<li>GooglePlay.FRIENDS  --leaderboard participants</li>
<li>GooglePlay.ALL_PLAYERS --leaderboard participants</li>
</ul>
<h2>Events</h2>

<h3>Game Events</h3>
<ul>
<li>Event.DATA_RECEIVED
<ul>
	<li>event.sender</li>
	<li>event.data</li>
</ul>
</li>
<li>Event.GAME_STARTED</li>
<li>Event.REPORT_ACHIEVEMENT_COMPLETE
<ul>
	<li>event.achievementId</li>
</ul>
</li>
<li>Event.LOAD_ACHIEVEMENTS_COMPLETE</li>
<li>Event.REPORT_SCORE_COMPLETE</li>
<li>Event.LOAD_SCORES_COMPLETE</li>
</ul>

<h3>Autorization Events</h3>
<ul>
<li>Event.LOGIN_ERROR</li>
<li>Event.LOGIN_COMPLETE</li>
</ul>

<h3>Room Events</h3>
<ul>
<li>Event.DISCONNECTED_FROM_ROOM
<ul>
	<li>event.roomId</li>
</ul>
</li>
<li>Event.ROOM_CREATED
<ul>
	<li>event.roomId</li>
</ul>
</li>
<li>Event.LEFT_ROOM
<ul>
	<li>event.roomId</li>
</ul>
</li>
<li>Event.ROOM_CONNECTED
<ul>
	<li>event.roomId</li>
</ul>
</li>
<li>Event.CONNECTED_TO_ROOM
<ul>
	<li>event.roomId</li>
</ul>
</li>
<li>Event.ROOM_CONNECTING
<ul>
	<li>event.roomId</li>
</ul>
</li>
<li>Event.ROOM_AUTO_MATCHING
<ul>
	<li>event.roomId</li>
</ul>
</li>
<li>Event.JOINED_ROOM
<ul>
	<li>event.roomId</li>
</ul>
</li>
<li>Event.INVITATION_RECEIVED
<ul>
	<li>event.invitationId (can be used to connect to room)</li>
</ul>
</li>
</ul>

<h3>Peer Events</h3>
<ul>
<li>Event.PEER_INVITED</li>
<li>Event.PEER_JOINED</li>
<li>Event.PEER_DISCONNECTED</li>
<li>Event.PEER_CONNECTED</li>
<li>Event.PEER_LEFT</li>
<li>Event.PEER_DECLINED</li>
</ul>
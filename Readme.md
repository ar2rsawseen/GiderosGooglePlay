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
googleplay:login() --login
googleplay:logout() --logout
googleplay:showSettings() --show settings screen
googleplay:showLeaderboard(leaderboardId) --show leaderboard screen
googleplay:reportScore(leaderboardId, score) --post score to service
googleplay:showAchievements() --show achievements screen
googleplay:reportAchievement(achId [, numSteps]) --achievement unlocked or progressed by specified amount of steps of total defined steps
googleplay:loadAchievements() --retrieve all defined achievemenets
googleplay:loadScores(leaderboardId [,timespan, participants, maxResults]) --retrieve all scores for specific leaderboard ina specific timespan for specific participans
googleplay:autoMatch(minPlayers, maxPlayers) --create a quick game between randomly selected players
googleplay:invitePlayers(minPlayers, maxPlayers) --allow user to invite players to a game
googleplay:joinRoom(invitationId) --joins the game you've been invited to
googleplay:showInvitations() --show screen for managing invitations
googleplay:showWaitingRoom(minPlayers) --show waiting room before the game with the list of players
googleplay:sendTo(playerId, data, isReliable) --send data to specific user by id
googleplay:sendToAll(data, isReliable) -- send to all users
googleplay:getCurrentPlayer() --get your current user name
googleplay:getAllPlayers() --retrieve information (id and name) for all users involved in game

<h2>Properties</h2>
GooglePlay.UNLOCKED --achievement state unlocked
GooglePlay.REVEALED --achievement state available to user
GooglePlay.HIDDEN   --achievement state unknown to user 

GooglePlay.ALL_TIME --leaderboard timespan all time
GooglePlay.WEEK     --leaderboard timespan week
GooglePlay.TODAY    --leaderboard timespan today

GooglePlay.FRIENDS  --leaderboard participants
GooglePlay.ALL_PLAYERS --leaderboard participants

<h2>Events</h2>

<h3>Game Events</h3>
Event.DATA_RECEIVED
	event.sender
	event.data
Event.GAME_STARTED
Event.REPORT_ACHIEVEMENT_COMPLETE
	event.achievementId
Event.LOAD_ACHIEVEMENTS_COMPLETE
Event.REPORT_SCORE_COMPLETE
Event.LOAD_SCORES_COMPLETE

<h3>Autorization Events</h3>
Event.LOGIN_ERROR
Event.LOGIN_COMPLETE

<h3>Room Events</h3>
Event.DISCONNECTED_FROM_ROOM
	event.roomId
Event.ROOM_CREATED
	event.roomId
Event.LEFT_ROOM
	event.roomId
Event.ROOM_CONNECTED
	event.roomId
Event.CONNECTED_TO_ROOM
	event.roomId
Event.ROOM_CONNECTING
	event.roomId
Event.ROOM_AUTO_MATCHING
	event.roomId
Event.JOINED_ROOM
	event.roomId
Event.INVITATION_RECEIVED
	event.invitationId (can be used to connect to room)

<h3>Peer Events</h3>
Event.PEER_INVITED
Event.PEER_JOINED
Event.PEER_DISCONNECTED
Event.PEER_CONNECTED
Event.PEER_LEFT
Event.PEER_DECLINED
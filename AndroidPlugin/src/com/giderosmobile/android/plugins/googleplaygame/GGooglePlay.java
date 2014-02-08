package com.giderosmobile.android.plugins.googleplaygame;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;

import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.appstate.OnStateDeletedListener;
import com.google.android.gms.appstate.OnStateLoadedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.achievement.OnAchievementUpdatedListener;
import com.google.android.gms.games.achievement.OnAchievementsLoadedListener;
import com.google.android.gms.games.leaderboard.Leaderboard;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.games.leaderboard.OnLeaderboardScoresLoadedListener;
import com.google.android.gms.games.leaderboard.OnScoreSubmittedListener;
import com.google.android.gms.games.leaderboard.SubmitScoreResult;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

public class GGooglePlay implements GameHelper.GameHelperListener, OnAchievementUpdatedListener, OnScoreSubmittedListener, OnAchievementsLoadedListener, OnLeaderboardScoresLoadedListener, RealTimeMessageReceivedListener,
RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener, OnStateLoadedListener, OnStateDeletedListener
{
	private static WeakReference<Activity> sActivity;
	private static GGooglePlay sInstance;
	protected static GameHelper mHelper;
	private static Room currentRoom;
	private static long sData;
	// We expose these constants here because we don't want users of this class
    // to have to know about GameHelper at all.
    public static final int CLIENT_GAMES = GameHelper.CLIENT_GAMES;
    public static final int CLIENT_APPSTATE = GameHelper.CLIENT_APPSTATE;
    public static final int CLIENT_PLUS = GameHelper.CLIENT_PLUS;
    public static final int CLIENT_ALL = GameHelper.CLIENT_ALL;
    final static int RC_UNUSED = 9002;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_WAITING_ROOM = 10002;
    
    private static boolean mWaitingRoomFinishedFromCode = false;
    // The participants in the currently active game
    private static ArrayList<Participant> mParticipants = null;
    // My participant ID in the currently active game
    private static String mMyId = null;

    // Requested clients. By default, that's just the games client.
    protected static int mRequestedClients = CLIENT_APPSTATE|CLIENT_GAMES|CLIENT_PLUS;
	
	public static void onCreate(Activity activity)
	{
		sActivity =  new WeakReference<Activity>(activity);
	}

	public static void onStart() {
		if(mHelper != null)
		{
			sActivity.get().runOnUiThread(new Runnable() {
	            public void run() {
	            	mHelper.onStart(sActivity.get());
	            }
	    	});
		}
	}

	public static void onStop() {
		if(mHelper != null)
			mHelper.onStop();
	}
	
	public static void onActivityResult(int request, int response, Intent data) {
		//Log.d("GiderosPlay", "onActivityResult");
		 if(mHelper != null)
			mHelper.onActivityResult(request, response, data);
		 if (request == RC_INVITATION_INBOX) {
			 if (response != Activity.RESULT_OK) {
				 // canceled
				 return;
			 }
			 Bundle extras = data.getExtras();
			 Invitation invitation =
		            extras.getParcelable(GamesClient.EXTRA_INVITATION);
			 if(invitation != null)
			 {
				 // accept it!
				 RoomConfig roomConfig = RoomConfig.builder(sInstance)
					 .setMessageReceivedListener(sInstance)
					 .setRoomStatusUpdateListener(sInstance)
					 .setInvitationIdToAccept(invitation.getInvitationId())
					 .build();
				 mHelper.getGamesClient().joinRoom(roomConfig);
			 }
		 }
		 else if (request == RC_SELECT_PLAYERS) {
			 //Log.d("GiderosPlay", "onSelectingPlayers: " + response);
			 if (response != Activity.RESULT_OK) {
				 // canceled
				 return;
			 }
			 //Log.d("GiderosPlay", "selecting");
			 // get the invitee list
			 final ArrayList<String> invitees =
					 data.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);

			 // get automatch criteria
			 Bundle autoMatchCriteria = null;
			 int minAutoMatchPlayers =
					 data.getIntExtra(GamesClient.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
			 int maxAutoMatchPlayers =
					 data.getIntExtra(GamesClient.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

			 if (minAutoMatchPlayers > 0) {
				 autoMatchCriteria =
						 RoomConfig.createAutoMatchCriteria(
								 minAutoMatchPlayers, maxAutoMatchPlayers, 0);
			 } else {
				 autoMatchCriteria = null;
			 }

			 // create the room and specify a variant if appropriate
			 RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(sInstance)
					 .setMessageReceivedListener(sInstance)
					 .setRoomStatusUpdateListener(sInstance);
			 roomConfigBuilder.addPlayersToInvite(invitees);
			 if (autoMatchCriteria != null) {
				 roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
			 }
			 RoomConfig roomConfig = roomConfigBuilder.build();
			 mHelper.getGamesClient().createRoom(roomConfig);
		 }
		 else if (request == RC_WAITING_ROOM) {
			 if (response == Activity.RESULT_OK) {
				 if (mWaitingRoomFinishedFromCode){
					 mWaitingRoomFinishedFromCode = false;
					 return;
				 }
				 broadcastStart();
				 if(sData != 0)
			        GGooglePlay.onGameStarted(sData);
			 	// (start game)
			 }
			 else if (response == Activity.RESULT_CANCELED) {
				 // Waiting room was dismissed with the back button. The meaning of this
				 // action is up to the game. You may choose to leave the room and cancel the
				 // match, or do something else like minimize the waiting room and
				 // continue to connect in the background.

				 // in this example, we take the simple approach and just leave the room:
				 mHelper.getGamesClient().leaveRoom(sInstance, currentRoom.getRoomId());
			 }
			 else if (response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
				 // player wants to leave the room.
				 mHelper.getGamesClient().leaveRoom(sInstance, currentRoom.getRoomId());
			 }
		 }
	}

	public static void broadcastStart() {
		String message = "__S";
        byte[] msg = message.getBytes(); 
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            mHelper.getGamesClient().sendReliableRealTimeMessage(null, msg, currentRoom.getRoomId(),
                    p.getParticipantId());
        }
    }
	
	static public void init(long data)
	{
		sData = data;
		sInstance = new GGooglePlay();
		mHelper = new GameHelper(sActivity.get());
		mHelper.setup(sInstance, mRequestedClients);
	}
	
	
	static public void cleanup()
	{
		if (sInstance != null)
    	{
    		sData = 0;
    		sInstance = null;
    	}
	}

    static public void onDestroy()
    {
    	cleanup(); 
    }
    
    static public boolean isAvailable(){
    	int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(sActivity.get());
    	if(result == ConnectionResult.SUCCESS)
    	{
    		return true;
    	}
    	return false;
    }
    
    static public void login(){
    	if(!mHelper.isSignedIn())
    	{
    		sActivity.get().runOnUiThread(new Runnable() {
               public void run() {
            	   mHelper.beginUserInitiatedSignIn();
               }
        	});
    	}
    }
    
    static public void logout(){
    	if(mHelper.isSignedIn())
    	{
    		sActivity.get().runOnUiThread(new Runnable() {
               public void run() {
            	   mHelper.signOut();
               }
        	});
    	}   
    }
    
    static public void showSettings(){
    	sActivity.get().runOnUiThread(new Runnable() {
            public void run() {
            	if(mHelper.isSignedIn())
            		sActivity.get().startActivityForResult(mHelper.getGamesClient().getSettingsIntent(), RC_UNUSED);
            }
    	});
    }
    
    static public void showLeaderboard(final String id){
    	sActivity.get().runOnUiThread(new Runnable() {
            public void run() {
            	if(mHelper.isSignedIn())
            		sActivity.get().startActivityForResult(mHelper.getGamesClient().getLeaderboardIntent(id), RC_UNUSED);
            }
    	});
    }
    
    static public void reportScore(String id, long score, int immediate){
    	if(mHelper.isSignedIn())
    	{
    		if(immediate == 1)
    		{
    			mHelper.getGamesClient().submitScoreImmediate(sInstance, id, score);
    		}
    		else
    		{
    			mHelper.getGamesClient().submitScore(id, score);
    		}
    	}
    }
    
    
    static public void showAchievements(){
    	sActivity.get().runOnUiThread(new Runnable() {
            public void run() {
            	if(mHelper.isSignedIn())
            		sActivity.get().startActivityForResult(mHelper.getGamesClient().getAchievementsIntent(), RC_UNUSED);
            }
    	});
    }
    
    static public void reportAchievement(String id, int immediate){
    	if(mHelper.isSignedIn())
    	{
    		if(immediate == 1)
    		{
    			mHelper.getGamesClient().unlockAchievementImmediate(sInstance, id);
    		}
    		else
    		{
    			mHelper.getGamesClient().unlockAchievement(id);
    		}
    	}
    }
    
    static public void reportAchievement(String id, int numSteps, int immediate){
    	if(mHelper.isSignedIn())
    	{
    		if(immediate == 1)
    		{
    			mHelper.getGamesClient().incrementAchievementImmediate(sInstance, id, numSteps);
    		}
    		else
    		{
    			mHelper.getGamesClient().incrementAchievement(id, numSteps);
    		}
    	}
    }
    
    static public void loadAchievements(){
    	if(mHelper.isSignedIn())
    		mHelper.getGamesClient().loadAchievements(sInstance, true);
    }
    
    static public void loadScores(String id, int span, int collection, int maxResults ){
    	if(mHelper.isSignedIn())
    		mHelper.getGamesClient().loadTopScores(sInstance, id, span, collection, maxResults);
    }
    
    static public void loadPlayerScores(String id, int span, int collection, int maxResults ){
    	if(mHelper.isSignedIn())
    		mHelper.getGamesClient().loadPlayerCenteredScores(sInstance, id, span, collection, maxResults);
    }
    
    /*CLOUD STUFF*/
    
    static public void loadState(int key){
    	if (mHelper.isSignedIn()) {
    		mHelper.getAppStateClient().loadState(sInstance, key);
    	}
    }
    
    static public void updateState(int key, byte[] state, int immediate){
    	if (mHelper.isSignedIn()) {
    		if(immediate == 1)
    		{
    			mHelper.getAppStateClient().updateStateImmediate(sInstance, key, state);
    		}
    		else
    		{
    			mHelper.getAppStateClient().updateState(key, state);
    		}
    	}
    }
    
    static public void resolveState(int key, String version, byte[] state){
    	if (mHelper.isSignedIn()) {
    		mHelper.getAppStateClient().resolveState(sInstance, key, version, state);
    	}
    }
    
    static public void deleteState(int key){
    	if (mHelper.isSignedIn()) {
    		mHelper.getAppStateClient().deleteState(sInstance, key);
    	}
    }
    
   /*MULTIPLAYER STUFF*/
    
    static public void autoMatch(int minPlayers, int maxPlayers) {
    	if(mHelper.isSignedIn())
    	{
    		// automatch criteria to invite 1 random automatch opponent.  
    		// You can also specify more opponents (up to 3). 
    		Bundle am = RoomConfig.createAutoMatchCriteria(minPlayers, maxPlayers, 0);

    		// build the room config:
    		RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(sInstance)
    				.setMessageReceivedListener(sInstance)
    				.setRoomStatusUpdateListener(sInstance);
    		roomConfigBuilder.setAutoMatchCriteria(am);
    		RoomConfig roomConfig = roomConfigBuilder.build();

    		// create room:
    		mHelper.getGamesClient().createRoom(roomConfig);
    	}

    }
    
    static public void invitePlayers(final int minPlayers, final int maxPlayers){
    	sActivity.get().runOnUiThread(new Runnable() {
            public void run() {
            	if(mHelper.isSignedIn())
            		sActivity.get().startActivityForResult(mHelper.getGamesClient().getSelectPlayersIntent(minPlayers, maxPlayers), RC_SELECT_PLAYERS);
            }
    	});
    }
    
    static public void joinRoom(String id){
    	if(mHelper.isSignedIn())
    	{
    		RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(sInstance)
                .setMessageReceivedListener(sInstance)
                .setRoomStatusUpdateListener(sInstance);
    		roomConfigBuilder.setInvitationIdToAccept(id);
    		mHelper.getGamesClient().joinRoom(roomConfigBuilder.build());
    	}
    }
    
    static public void showInvitations(){
    	sActivity.get().runOnUiThread(new Runnable() {
            public void run() {
            	if(mHelper.isSignedIn())
            		sActivity.get().startActivityForResult(mHelper.getGamesClient().getInvitationInboxIntent(), RC_WAITING_ROOM);
            }
    	});
    }
    
    static public void showWaitingRoom(final int minPlayers){
    	sActivity.get().runOnUiThread(new Runnable() {
            public void run() {
            	if(mHelper.isSignedIn() && currentRoom != null)
            		sActivity.get().startActivityForResult(mHelper.getGamesClient().getRealTimeWaitingRoomIntent(currentRoom, minPlayers), RC_WAITING_ROOM);
            }
    	});
    }
    
    static public void sendTo(String id, byte[] message, int isReliable){
    	if(mHelper.isSignedIn())
    	{
    		if(isReliable == 1){
    			mHelper.getGamesClient().sendReliableRealTimeMessage(null, message, currentRoom.getRoomId(), id);
    		}
    		else
    		{
    			mHelper.getGamesClient().sendUnreliableRealTimeMessage(message, currentRoom.getRoomId(), id);
    		}
    	}
    }
    
    static public void sendToAll(byte[] message, int isReliable){
    	if(mHelper.isSignedIn())
    	{
    		if(isReliable == 1){
    			for (Participant p : mParticipants) {
    				if (!p.getParticipantId().equals(mMyId)) {
    					mHelper.getGamesClient().sendReliableRealTimeMessage(null, message, currentRoom.getRoomId(), p.getParticipantId());
    				}
    			}
    		}
    		else
    		{
    			mHelper.getGamesClient().sendUnreliableRealTimeMessageToAll(message, currentRoom.getRoomId());
    		}
    	}
    }
    
    static public String getCurrentPlayer(){
    	if(mHelper.isSignedIn())
    		return mHelper.getGamesClient().getCurrentPlayer().getDisplayName();
    	return "";
    }
    
    static public String getCurrentPlayerId(){
    	if(mHelper.isSignedIn())
    		return mHelper.getGamesClient().getCurrentPlayer().getPlayerId();
    	return "";
    }
    
    static public Object getAllPlayers(){
    	SparseArray<Bundle> arr = new SparseArray<Bundle>();
    	if(mParticipants != null )
    	{
    		int i = 1;
    		for (Participant p : mParticipants) {
        		Bundle bundle = new Bundle();
        		bundle.putString("id", p.getParticipantId());
        		bundle.putString("name", p.getDisplayName());
    			arr.put(i, bundle);
    			i++;
    		}
    	}
    	return arr;
    }
    
    static public void updateRoom(Room room) {
        mParticipants = room.getParticipants();
    }
    
	@Override
 	public void onSignInFailed() {
		if (sData != 0)
			GGooglePlay.onSignInFailed(sData);
	}

	@Override
	public void onSignInSucceeded() {
		if (sData != 0)
		{
			GGooglePlay.onSignInSucceeded(sData);
			mHelper.getGamesClient().registerInvitationListener(sInstance);
			 if (mHelper.getInvitationId() != null) {
				 //invitation already there
				 GGooglePlay.onInvitationReceived(mHelper.getInvitationId(), sData);
			 }
		}
	}

	@Override
	public void onAchievementUpdated(int statusCode, String achievementId) {
		if (sData != 0)
		{
			if(statusCode == GamesClient.STATUS_OK || statusCode == GamesClient.STATUS_ACHIEVEMENT_UNLOCKED){
				GGooglePlay.onAchievementUpdated(achievementId, sData);
			}
		}
		
	}
	//check if successful
	
	@Override
	public void onScoreSubmitted(int statusCode, SubmitScoreResult result) {
		if (sData != 0)
		{
			if(statusCode == GamesClient.STATUS_OK){
				GGooglePlay.onScoreSubmitted(sData);
			}
		}
	}

	
	@Override
	public void onAchievementsLoaded(int statusCode, AchievementBuffer buffer) {
		if (sData != 0)
		{
			if(statusCode == GamesClient.STATUS_OK){
				SparseArray<Bundle> arr = new SparseArray<Bundle>();
				int size = buffer.getCount();
				for(int i = 0; i < size; i++){
					Achievement ach = buffer.get(i);
					Bundle map = new Bundle();
					map.putString("id", ach.getAchievementId());
					map.putString("description", ach.getDescription());
					map.putString("name", ach.getName());
					map.putInt("lastUpdate", (int)(ach.getLastUpdatedTimestamp()/1000));
					map.putInt("status", ach.getState()); //0-unlocked, 1-revealed, 2-hidden
					if(ach.getType() == Achievement.TYPE_INCREMENTAL)
					{
						map.putInt("currentSteps", ach.getCurrentSteps());
						map.putInt("totalSteps", ach.getTotalSteps());
					}
					arr.put(i, map);
				}
				GGooglePlay.onAchievementsLoaded(arr, sData);
			}
		}
		buffer.close();
	}
	
	@Override
	public void onLeaderboardScoresLoaded(int statusCode, Leaderboard lb, LeaderboardScoreBuffer scores) {
		if (sData != 0)
		{
			if(statusCode == GamesClient.STATUS_OK){
				String leaderboardId =  lb.getLeaderboardId();
				String leaderboardName = lb.getDisplayName();
	
				SparseArray<Bundle> lscores = new SparseArray<Bundle>();
				int size = scores.getCount();
				for(int i = 0; i < size; i++){
					LeaderboardScore l = scores.get(i);
					Bundle map = new Bundle();
					map.putString("rank", l.getDisplayRank());
					map.putString("score", l.getDisplayScore());
					map.putString("name", l.getScoreHolderDisplayName());
					map.putString("playerId", l.getScoreHolder().getPlayerId());
					map.putInt("timestamp", (int)(l.getTimestampMillis()/1000));
					lscores.put(i, map);
				}
				GGooglePlay.onLeaderboardScoresLoaded(leaderboardId, leaderboardName, lscores, sData);
			}
		}
		scores.close();
	}	
	
	
	@Override
	public void onInvitationReceived(Invitation invitation) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/Invitation.html
		if (sData != 0)
			GGooglePlay.onInvitationReceived(invitation.getInvitationId(), sData);
	}
	
	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/realtime/RoomUpdateListener.html
		if (statusCode != GamesClient.STATUS_OK) {
			// display error
			return;  
		}
		currentRoom = room;
		updateRoom(room);
		if (sData != 0)
			GGooglePlay.onJoinedRoom(room.getRoomId(), sData);
	}

	@Override
	public void onLeftRoom(int statusCode, String roomId) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/realtime/RoomUpdateListener.html
		if (statusCode != GamesClient.STATUS_OK) {
			// display error
			return;  
		}
		currentRoom = null;
		if (sData != 0)
			GGooglePlay.onLeftRoom(roomId, sData);
	}

	@Override
	public void onRoomConnected(int statusCode, Room room) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/realtime/RoomUpdateListener.html
		if (statusCode != GamesClient.STATUS_OK) {
            return;
        }
        updateRoom(room);
		if (sData != 0)
			GGooglePlay.onRoomConnected(room.getRoomId(), sData);
	}

	@Override
	public void onRoomCreated(int statusCode, Room room) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/realtime/RoomUpdateListener.html
		if (statusCode != GamesClient.STATUS_OK) {
			// display error
			return;  
		}
		currentRoom = room;
		updateRoom(room);
		if (sData != 0)
			GGooglePlay.onRoomCreated(room.getRoomId(), sData);
	}

	@Override
	public void onConnectedToRoom(Room room) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/realtime/RoomStatusUpdateListener.html
		mParticipants = room.getParticipants();
        mMyId = room.getParticipantId(mHelper.getGamesClient().getCurrentPlayerId());
        if (sData != 0)
			GGooglePlay.onConnectedToRoom(room.getRoomId(), sData);
	}

	@Override
	public void onDisconnectedFromRoom(Room room) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/realtime/RoomStatusUpdateListener.html
		mHelper.getGamesClient().leaveRoom(sInstance, room.getRoomId());
		currentRoom = null;
		if (sData != 0)
			GGooglePlay.onDisconnectedFromRoom(room.getRoomId(), sData);
	}

	@Override
	public void onPeerDeclined(Room arg0, List<String> arg1) {
		updateRoom(arg0);
		if (sData != 0)
			GGooglePlay.onPeerDeclined(sData);
	}

	@Override
	public void onPeerInvitedToRoom(Room arg0, List<String> arg1) {
		updateRoom(arg0);
		if (sData != 0)
			GGooglePlay.onPeerInvitedToRoom(sData);
	}

	@Override
	public void onPeerJoined(Room arg0, List<String> arg1) {
		updateRoom(arg0);
		if (sData != 0)
			GGooglePlay.onPeerJoined(sData);
	}

	@Override
	public void onPeerLeft(Room arg0, List<String> arg1) {
		updateRoom(arg0);
		if (sData != 0)
			GGooglePlay.onPeerLeft(sData);
	}

	@Override
	public void onPeersConnected(Room arg0, List<String> arg1) {
		updateRoom(arg0);
		if (sData != 0)
			GGooglePlay.onPeersConnected(sData);
	}

	@Override
	public void onPeersDisconnected(Room arg0, List<String> arg1) {
		updateRoom(arg0);
		if (sData != 0)
			GGooglePlay.onPeersDisconnected(sData);
	}

	@Override
	public void onRoomAutoMatching(Room room) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/realtime/RoomStatusUpdateListener.html
		updateRoom(room);
		if (sData != 0)
			GGooglePlay.onRoomAutoMatching(room.getRoomId(), sData);
	}

	@Override
	public void onRoomConnecting(Room room) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/realtime/RoomStatusUpdateListener.html
		updateRoom(room);
		if (sData != 0)
			GGooglePlay.onRoomConnecting(room.getRoomId(), sData);
	}

	@Override
	public void onRealTimeMessageReceived(RealTimeMessage rtm) {
		//http://developer.android.com/reference/com/google/android/gms/games/multiplayer/realtime/RealTimeMessageReceivedListener.html
		// get real-time message
	    byte[] b = rtm.getMessageData();
	    String message = b.toString();
	    if (message.equals("__S")) {
            // someone else started to play -- so dismiss the waiting room and
            // get right to it!
	    	mWaitingRoomFinishedFromCode = true;
	        sActivity.get().finishActivity(RC_WAITING_ROOM);
	        if(sData != 0)
	        	GGooglePlay.onGameStarted(sData);
        }
	    else
	    {
	    	if (sData != 0)
	    	{
	    		String sender = rtm.getSenderParticipantId();
	    		GGooglePlay.onDataReceived(b, sender, sData);
	    	}
	    }
	}
	
	@Override
	public void onStateConflict(int stateKey, String ver,
            byte[] localData, byte[] serverData) {
		if (sData != 0)
			GGooglePlay.onStateConflict(stateKey, ver, localData, serverData, sData);
	}

	@Override
	public void onStateLoaded(int statusCode, int stateKey, byte[] data) {
		if (statusCode == AppStateClient.STATUS_OK) {
			if (sData != 0)
				GGooglePlay.onStateLoaded(stateKey, data, 1, sData);
		}
		else if (statusCode == AppStateClient.STATUS_NETWORK_ERROR_STALE_DATA) {
			if (sData != 0)
				GGooglePlay.onStateLoaded(stateKey, data, 0, sData);
		} else {
			if (sData != 0)
			{
				String error = "unknown error";
				if(statusCode == AppStateClient.STATUS_CLIENT_RECONNECT_REQUIRED)
				{
					error = "need to reconnect client";
				}
				else if(statusCode == AppStateClient.STATUS_NETWORK_ERROR_OPERATION_FAILED)
				{
					error = "could not connect server";
				}
				else if(statusCode == AppStateClient.STATUS_NETWORK_ERROR_NO_DATA)
				{
					error = "could not connect server";
				}
				else if(statusCode == AppStateClient.STATUS_STATE_KEY_NOT_FOUND)
				{
					error = "no data";
				}
				else if(statusCode == AppStateClient.STATUS_STATE_KEY_LIMIT_EXCEEDED)
				{
					error = "key limit exceeded";
				}
				else if(statusCode == AppStateClient.STATUS_WRITE_SIZE_EXCEEDED)
				{
					error = "too much data passed";
				}
				GGooglePlay.onStateError(stateKey, error, sData);
			}
		}
	}
	
	@Override
	public void onStateDeleted(int statusCode, int stateKey) {
		if (statusCode == AppStateClient.STATUS_OK) {
			if (sData != 0)
				GGooglePlay.onStateDeleted(stateKey, sData);
		}
		else
		{
			if (sData != 0)
			{
				String error = "unknown error";
				if(statusCode == AppStateClient.STATUS_CLIENT_RECONNECT_REQUIRED)
				{
					error = "need to reconnect client";
				}
				else if(statusCode == AppStateClient.STATUS_NETWORK_ERROR_OPERATION_FAILED)
				{
					error = "could not connect server";
				}
				GGooglePlay.onStateError(stateKey, error, sData);
			}
		}
		
	}
	
	private static native void onSignInFailed(long data);
	private static native void onSignInSucceeded(long data);
	private static native void onAchievementUpdated(String id, long data);
	private static native void onScoreSubmitted(long data);
	private static native void onAchievementsLoaded(Object arr, long data);
	private static native void onLeaderboardScoresLoaded(String id, String name, Object scores, long data);
	
	private static native void onStateLoaded(int key, byte[] state, int fresh, long data);
	private static native void onStateError(int key, String error, long data);
	private static native void onStateConflict(int key, String ver, byte[] localState, byte[] serverState, long data);
	private static native void onStateDeleted(int key, long data);
	
	private static native void onGameStarted(long data);
	private static native void onInvitationReceived(String id, long data);
	private static native void onJoinedRoom(String id, long data);
	private static native void onLeftRoom(String id, long data);
	private static native void onRoomConnected(String id, long data);
	private static native void onRoomCreated(String id, long data);
	private static native void onConnectedToRoom(String id, long data);
	private static native void onDisconnectedFromRoom(String id, long data);
	private static native void onPeerDeclined(long data);
	private static native void onPeerInvitedToRoom(long data);
	private static native void onPeerJoined(long data);
	private static native void onPeerLeft(long data);
	private static native void onPeersConnected(long data);
	private static native void onPeersDisconnected(long data);
	private static native void onRoomAutoMatching(String id, long data);
	private static native void onRoomConnecting(String id, long data);
	private static native void onDataReceived(byte[] message, String sender, long data);

	@Override
	public void onP2PConnected(String participantId) {}

	@Override
	public void onP2PDisconnected(String participantId) {}

	@Override
	public void onInvitationRemoved(String arg0) {}
}

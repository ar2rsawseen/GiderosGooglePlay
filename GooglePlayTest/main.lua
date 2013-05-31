function print_r (t, indent, done)
  done = done or {}
  indent = indent or ''
  local nextIndent -- Storage for next indentation value
  for key, value in pairs (t) do
    if type (value) == "table" and not done [value] then
      nextIndent = nextIndent or
          (indent .. string.rep(' ',string.len(tostring (key))+2))
          -- Shortcut conditional allocation
      done [value] = true
      print (indent .. "[" .. tostring (key) .. "] => Table {");
      print  (nextIndent .. "{");
      print_r (value, nextIndent .. string.rep(' ',2), done)
      print  (nextIndent .. "}");
    else
      print  (indent .. "[" .. tostring (key) .. "] => " .. tostring (value).."")
    end
  end
end

require "googleplay"


googleplay:addEventListener(Event.LOGIN_ERROR, function()
	print("LOGIN_ERROR")
end)

googleplay:addEventListener(Event.LOGIN_COMPLETE, function()
	print("LOGIN_COMPLETE")
	--googleplay:showSettings()
	--googleplay:reportScore("CgkIq_W6x6sQEAIQBA", 100)
	--googleplay:showLeaderboard("CgkIq_W6x6sQEAIQBA")
	--googleplay:reportAchievement("CgkIq_W6x6sQEAIQAQ")
	--googleplay:loadAchievements()
	googleplay:loadScores("CgkIq_W6x6sQEAIQBA")
	
end)

googleplay:addEventListener(Event.LOAD_SCORES_COMPLETE, function(e)
	print("LOAD_SCORES_COMPLETE")
	print_r(e)
end)

googleplay:addEventListener(Event.REPORT_SCORE_COMPLETE, function()
	print("REPORT_SCORE_COMPLETE")
end)

googleplay:addEventListener(Event.LOAD_ACHIEVEMENTS_COMPLETE, function()
	print("LOAD_ACHIEVEMENTS_COMPLETE")
end)

googleplay:addEventListener(Event.REPORT_ACHIEVEMENT_COMPLETE, function()
	print("REPORT_ACHIEVEMENT_COMPLETE")
end)

googleplay:login()
--googleplay:showSettings()
package com.codesmashers.doodle

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognizerIntent
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.codesmashers.doodle.adapters.ChatAdapter
import com.codesmashers.doodle.adapters.PlayingPlayersAdapter
import com.codesmashers.doodle.adapters.WinningListAdapters
import com.codesmashers.doodle.model.ChatText
import com.codesmashers.doodle.model.Information
import com.codesmashers.doodle.model.playerInfo
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.chats_preview.*
import kotlinx.android.synthetic.main.choose_word.*
import kotlinx.android.synthetic.main.dialog_more.*
import kotlinx.android.synthetic.main.game_content.*
import kotlinx.android.synthetic.main.dialog_winners.*
import kotlinx.android.synthetic.main.layout_header.*
import java.util.*
import kotlin.random.Random


/** [GameActivity] is where actual  game commence. */
class GameActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_STT = 1
    }
    private var booleanForGameEnd=false
    private var wordGuessedOrNot: Boolean = false
    private lateinit var countdownTimer: CountDownTimer
    var booleanForCountdownStartedOrNot: Boolean = false
    var booleanForCountdownCancelled: Boolean = false
    private var guessingWord: String = ""
    private lateinit var valueEventListenerForGuessingWord: ValueEventListener
    private lateinit var guessingWordRef: DatabaseReference
    private lateinit var paintView: PaintView
    private var roundTillNow = 0
    var flagtime = false
    private lateinit var valueEventListenerForWhoesChance: ValueEventListener
    private lateinit var whoseChanceRef: DatabaseReference
    private var indexOfChance = -1
    private lateinit var valueEventListenerForChanceChange: ValueEventListener
    private lateinit var chanceChangeRef: DatabaseReference
    private var serverHost: Int = 0
    private var userName = ""
    private var userId = ""
    private lateinit var prefs: SharedPreferences
    private lateinit var database: DatabaseReference
    private lateinit var postReference: DatabaseReference
    private lateinit var playerReference: DatabaseReference
    private lateinit var childEventListenerForChat: ChildEventListener
    private lateinit var childEventListenerForPlayers: ChildEventListener
    private lateinit var childEventListenerForGame: ChildEventListener
    private lateinit var chatAdapter: ChatAdapter
    var reference: String? = ""
    private var otherUserName: String? = ""
    private var stringDisplay = ""
    private var playersList = mutableListOf<playerInfo>()
    private var chatsDisplay = mutableListOf<ChatText>()
    var playerCount: Long = 0
    private lateinit var postRef: DatabaseReference
    private val baseCount: Long = 1
    private var goToMainActivityBoolean: Boolean = false
    private var backButtonPressedBoolean: Boolean = false
    private var host = 0
    var timeLimit: Long = 0
    var noOfRounds = 0
    val fixedScore = 10
    private lateinit var mDialog: Dialog
    var colorProvider = mutableListOf<Boolean>() // colored List of Players for Navigational Drawer
    var wordsCollection = WordCollectionData().wordsCollection
    var colorSetForChats = mutableSetOf<String>() //contains UID of colored players in Chats.
    var hostUID: String = ""
    var numGuesPlayer = 0
   // var mcolor  = Color.BLACK
    private lateinit var RoundChangeRef: DatabaseReference
    private lateinit var valueEventListenerForRoundChange: ValueEventListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        /** To setup [PaintView]. The canvas of drawing. */
        paintView = PaintView(this)
        main.addView(paintView)

        /** To clear the canvas. */
        eraser.setOnClickListener {
            if (host == 1) {
                paintView.clear()
                postRef.push().setValue(Information(10001f, 10001f, 3))
                paintView.isclear = 1
            }
        }

        leave_btn.setOnClickListener {
            super.onBackPressed()
            backButtonPressedBoolean = true
            routeToMainActivity()
        }

        peoples.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }



        prefs = this.getSharedPreferences(
            getString(R.string.packageName), Context.MODE_PRIVATE
        )

        prefs.edit().putInt(getString(R.string.scoreOfCurPlayer), 0).apply()
        userId = prefs.getString(getString(R.string.userId), getString(R.string.EMPTY))!!
        userName = prefs.getString(getString(R.string.userName), getString(R.string.EMPTY))!!
        val intent: Intent = intent
        reference = intent.getStringExtra(getString(R.string.reference))
        serverHost = intent.getIntExtra(getString(R.string.host), 0)
        noOfRounds = intent.getIntExtra(getString(R.string.rounds), 0)
        timeLimit = intent.getLongExtra(getString(R.string.countdown), 0)
        timeLimit *= 1000
        paintView.end(0f, 0f)
        paintView.getRef(reference)

        playing_players.layoutManager = LinearLayoutManager(this)

        chatAdapter = ChatAdapter(chats = chatsDisplay, colorSet = colorSetForChats)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        chats_recycler.layoutManager = layoutManager
        chats_recycler.adapter = chatAdapter

        database = Firebase.database.reference
        if (reference != null) {

            if (serverHost == 1) {
                database.child(getString(R.string.rooms)).child(reference.toString())
                    .child(getString(R.string.info))
                    .child(getString(R.string.chanceChange)).setValue(1)
                paintView.host = 1
                host = 1
            }

            /** Chat event listener for a room. */
            chatListener()

            /** Drawing data event listener of a room. */
            drawingDataEventListener()

            /** Player event listener in a room. */
            playerEventListener()

            /** Chance Change Event Listener of a room. */
            chanceChangeEventListener()

            /** Whose Chance Event Listener of a room. */
            whoseChanceEventListener()

            /** Word guessing Event Listener of a room. */
            guessingWordEventListener()

            rounds_left.text = "1"

            /** For Displaying the current round */
            changeCurrentRoundListener()

        }
        mic.setOnClickListener {
            // Get the Intent action
            val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            // Language model defines the purpose, there are special models for other use cases, like search.
            sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Adding an extra language, you can use any language from the Locale class.
            sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            // Text that shows up on the Speech input prompt.
            sttIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now!")
            try {
                // Start the intent for a result, and pass in our request code.
                startActivityForResult(sttIntent, REQUEST_CODE_STT)
            } catch (e: ActivityNotFoundException) {
                // Handling error when the service is not available.
                e.printStackTrace()
                Toast.makeText(this, "Your device does not support STT.", Toast.LENGTH_LONG).show()
            }
        }


        /** Chat send button. */
        button.setOnClickListener {
            if (editText.text.toString() == "") {
                Toast.makeText(this, getString(R.string.emptyText), Toast.LENGTH_SHORT).show()
            } else {
                if (reference != null) {
                    var wordToUpload = getString(R.string.wordGuessed)
                    if (editText.text.toString()
                            .toLowerCase() != guessingWord.toLowerCase() || host == 1 || wordGuessedOrNot
                    ) {
                        wordToUpload = editText.text.toString().trim()
                    } else {
                        var curScore = prefs.getInt(getString(R.string.scoreOfCurPlayer), 0)
                        curScore += fixedScore
                        database.child(getString(R.string.rooms)).child(reference.toString())
                            .child(getString(R.string.Players))
                            .child(userId).child(getString(R.string.score)).setValue(curScore)
                        prefs.edit().putInt(getString(R.string.scoreOfCurPlayer), curScore).apply()
                        wordGuessedOrNot = true
                    }
                    if (editText.text.toString() == getString(R.string.wordGuessed)) {
                        wordToUpload = getString(R.string.wordGuessedModified)
                    }
                    chatUploadToDatabase(wordToUpload)
                    editText.text.clear()
                    chatAdapter.notifyDataSetChanged()
                    chats_recycler.scrollToPosition(chatsDisplay.size - 1)
                } else {
                    Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }


    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // Handle the result for our request code.
            REQUEST_CODE_STT -> {
                // Safety checks to ensure data is available.
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Retrieve the result array.
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    // Ensure result array is not null or empty to avoid errors.
                    if (!result.isNullOrEmpty()) {
                        // Recognized text is in the first position.
                        val recognizedText = result[0]
                        // Do what you want with the recognized text.
                        editText.setText(recognizedText)
                        onResume()
                    }
                }
            }
        }
    }

    private fun scoreBoard() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_winners)

        var winnersList: MutableList<playerInfo> = playersList.toMutableList()
        winnersList = winnersList.sortedWith(compareBy { it.score }).reversed().toMutableList()

        val window = dialog.window
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
        val layoutManager = LinearLayoutManager(this)
        dialog.winners_list.layoutManager = layoutManager
        val winningListAdapters = WinningListAdapters(winnersList)
        dialog.winners_list.adapter = winningListAdapters

        dialog.return_back.setOnClickListener {
            dialog.dismiss()
            super.onBackPressed()
            backButtonPressedBoolean = true
            routeToMainActivity()
        }
    }

    /** Chat event listener for a room. */
    private fun chatListener() {
        postReference = database.child(getString(R.string.games)).child(reference.toString())
            .child(getString(R.string.Chats))

        childEventListenerForChat = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val textObj = snapshot.getValue<ChatText>()
                if (textObj != null) {
                    if (textObj.text == getString(R.string.wordGuessed)) {
                        colorSetForChats.add(textObj.UID)
                        for (i in 0 until playersList.size) {
                            if (playersList[i].UID == textObj.UID) {
                                colorProvider[i] = true
                                if (host == 1)
                                    updateDrawingValue()
                            }
                        }
                        val adapter = PlayingPlayersAdapter(playersList, colorProvider)
                        playing_players.adapter = adapter
                    }
                    otherUserName = textObj.userName
                    chatsDisplay.add(ChatText(textObj.UID, textObj.userName, textObj.text))
                    chatAdapter.notifyDataSetChanged()
                    chats_recycler.scrollToPosition(chatsDisplay.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // not needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // not needed
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // not needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // not needed
            }

        }
        postReference.addChildEventListener(childEventListenerForChat)
    }

    /** Drawing data event listener of a room. */
    private fun drawingDataEventListener() {
        postRef = database.child(getString(R.string.drawingData)).child(reference.toString())
        childEventListenerForGame = object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val info = snapshot.getValue<Information>()
                Log.i(
                    "DOWNLOADORNOT",
                    info!!.type.toString() + " " + info.pointX.toString() + " " + info.pointY.toString()
                )
                if (info.type == 0) {
                    paintView.start(info.pointX, info.pointY)
                } else if (info.type == 2) {
                    paintView.co(info!!.pointX, info.pointY)
                } else if (info.type == 1) {
                    paintView.end(info!!.pointX, info.pointY)
                } else if (info.type == 3) {
                    paintView.clear()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // not needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // not needed
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // not needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // not needed
            }

        }
        postRef.addChildEventListener(childEventListenerForGame)
    }

    private fun updateDrawingValue() {
        var curScore = prefs.getInt(getString(R.string.scoreOfCurPlayer), 0)
        curScore += 2
        prefs.edit().putInt(getString(R.string.scoreOfCurPlayer), curScore).apply()
        database.child(getString(R.string.rooms)).child(reference.toString())
            .child(getString(R.string.Players))
            .child(userId).child(getString(R.string.score)).setValue(curScore)
    }

    private fun updateScoreToLocalList(playerInfoObj: playerInfo) {
        for (i in 0 until playersList.size) {
            if (playerInfoObj.UID.toString() == playersList[i].UID) {
                playersList[i].score = playerInfoObj.score
            }
        }
    }

    /** Player event listener in a room. */
    private fun playerEventListener() {
        playerReference = database.child(getString(R.string.rooms)).child(reference.toString())
            .child(getString(R.string.Players))
        childEventListenerForPlayers = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val playerInfoObj = snapshot.getValue<playerInfo>()
                if (playerInfoObj != null) {
                    playersList.add(playerInfoObj)
                    colorProvider.add(false)
                    playerCount++
                    val adapter = PlayingPlayersAdapter(playersList, colorProvider)
                    playing_players.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // not needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // not needed
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val playerInfoObj = snapshot.getValue<playerInfo>()
                Log.i("CHILD CHANGE", "INSIDE")
                if (serverHost == 1) {
                    Log.i("SERVER HOST", "INSIDE")
                    Log.i("HOST UID", hostUID)
                    Log.i("PLAYER UID", playerInfoObj?.UID!!)
                    if (playerInfoObj.UID != hostUID) {
                        Log.i("INFO OBJ", "INSIDE")
                        numGuesPlayer++
                        cancelCountdownAndNextChance()
                    }
                }
                updateScoreToLocalList(playerInfoObj!!)
                if(booleanForGameEnd==true){
                    scoreBoard()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val playerInfoObj = snapshot.getValue<playerInfo>()
                if (playerInfoObj != null) {
                    val temp = mutableListOf<playerInfo>()
                    val tempColor = mutableListOf<Boolean>()
                    for (i in 0 until playersList.size) {
                        if (playersList[i].UID != playerInfoObj.UID) {
                            temp.add(playersList[i])
                            tempColor.add(colorProvider[i])
                            Log.i("AAJAJA", playersList[i].Name)
                        }
                    }
                    playersList = temp
                    colorProvider = tempColor
                    playerCount--
                    val adapter = PlayingPlayersAdapter(playersList, colorProvider)
                    playing_players.adapter = adapter
                    for (i in 0 until playersList.size) {
                        Log.i("playersdel", playersList[i].Name)
                    }
                }
            }

        }
        playerReference.addChildEventListener(childEventListenerForPlayers)
    }

    private fun cancelCountdownAndNextChance() {
        if (numGuesPlayer == playersList.size - 1) {
            numGuesPlayer = 0
            Log.i("CANCEL COUNTDOWN", booleanForCountdownStartedOrNot.toString())
            if (booleanForCountdownStartedOrNot) {
                timer_xml.text = " "
                word_xml.text = ""
                stringDisplay = ""
                flagtime = false
                countdownTimer.cancel()
                countdownTimer.onFinish()
            }
            booleanForCountdownStartedOrNot = false
        }
    }

    /** Chance Change Event Listener of a room. */
    private fun chanceChangeEventListener() {
        chanceChangeRef = database.child(getString(R.string.rooms)).child(reference.toString())
            .child(getString(R.string.info))
            .child(getString(R.string.chanceChange))
        valueEventListenerForChanceChange = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value.toString() == "1") {
                    if (serverHost == 1) {
                        changeUserChance()
                    } else {
                        if (booleanForCountdownStartedOrNot) {
                            booleanForCountdownStartedOrNot = false
                            countdownTimer.cancel()
                            timer_xml.text = " "
                            word_xml.text = " "
                            flagtime = false
                        }
                    }
                }
            }
        }
        chanceChangeRef.addValueEventListener(valueEventListenerForChanceChange)
    }

    /** Whose Chance Event Listener of a room. */
    private fun whoseChanceEventListener() {
        whoseChanceRef =
            database.child(getString(R.string.rooms)).child(reference.toString())
                .child(getString(R.string.info)).child(getString(R.string.chanceUID))
        valueEventListenerForWhoesChance = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value.toString() == userId) {
                    Toast.makeText(this@GameActivity, "MY CHANCE", Toast.LENGTH_SHORT).show()
                    chatUploadToDatabase(userName.trim() + " chance!!")
                    host = 1
                    paintView.host = 1
                    setDialog()
                } else {
                    host = 0
                    paintView.host = 0
                }
                for (i in 0 until colorProvider.size) {
                    colorProvider[i] = false
                    val adapter = PlayingPlayersAdapter(playersList, colorProvider)
                    playing_players.adapter = adapter
                }
                wordGuessedOrNot = false
                colorSetForChats.clear()
            }
        }
        whoseChanceRef.addValueEventListener(valueEventListenerForWhoesChance)
    }

    /** Word guessing Event Listener of a room. */
    private fun guessingWordEventListener() {
        var flag = 0
        guessingWordRef =
            database.child(getString(R.string.rooms)).child(reference.toString())
                .child(getString(R.string.info))
                .child(getString(R.string.wordToGuess))
        valueEventListenerForGuessingWord = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                if (flag == 0) {
                    flag = 1
                } else if (!booleanForCountdownCancelled) {
                    countdown(timeLimit)
                }

                if (host == 0) {
                    guessingWord = snapshot.value.toString()
                    stringDisplay = ""
                    for (i in guessingWord.indices) {
                        stringDisplay += '_'
                    }
                } else {
                    stringDisplay = getString(R.string.drawNow)
                }
                word_xml.text = stringDisplay
            }
        }
        guessingWordRef.addValueEventListener(valueEventListenerForGuessingWord)
    }

    /** Dialog to choose one of the Three words given as a choice to the player from the collection. */
    private fun setDialog() {
        mDialog = Dialog(this)
        mDialog.setContentView(R.layout.choose_word)
        wordsCollection.shuffle()
        val first = 0
        val second = 1
        val third = 2

        for (i in 0 until wordsCollection.size) {
            if (i == first){

                mDialog.word1.text = wordsCollection[0]
            val name = wordsCollection[0]
                if (name.equals("bat")) {
                mDialog.img1.setImageResource(R.drawable.bat)
            }
            else if (name.equals("ball")) {
                mDialog.img1.setImageResource(R.drawable.ball)
            }
            else  if (name.equals("flag")) {
                mDialog.img1.setImageResource(R.drawable.flag)
            }
            else  if (name == "apple") {
                mDialog.img1.setImageResource(R.drawable.apple)
            }
            else  if (name == "baloon") {
                mDialog.img1.setImageResource(R.drawable.baloon)
            }
            else  if (name == "boat") {
                mDialog.img1.setImageResource(R.drawable.boat)
            }
            else if (name == "door") {
                mDialog.img1.setImageResource(R.drawable.door)
            }
            else if (name == "egg") {
                mDialog.img1.setImageResource(R.drawable.egg)
            }
            else if (name == "arrow") {
                mDialog.img1.setImageResource(R.drawable.arrow)
            }
            else if (name == "audi") {
                mDialog.img1.setImageResource(R.drawable.audi)
            }
            else if (name == "love") {
                mDialog.img1.setImageResource(R.drawable.ic_baseline_favorite_24)
            }
            else if (name == "nike") {
                mDialog.img1.setImageResource(R.drawable.nike)
            }
            else if (name == "rocket") {
                mDialog.img1.setImageResource(R.drawable.rocket)
            }
            else if (name.equals("gun")) {
                mDialog.img1.setImageResource(R.drawable.gun)
            }
        }
            else if (i == second) {


                mDialog.word2.text = wordsCollection[1]

                val nam = wordsCollection[1]
                if (nam.equals("bat")) {
                    mDialog.img2.setImageResource(R.drawable.bat)
                }
                else if (nam.equals("ball")) {
                    mDialog.img2.setImageResource(R.drawable.ball)
                }
                else if (nam.equals("flag")) {
                    mDialog.img2.setImageResource(R.drawable.flag)
                }
                else if (nam == "apple") {
                    mDialog.img2.setImageResource(R.drawable.apple)
                }
                else if (nam == "baloon") {
                    mDialog.img2.setImageResource(R.drawable.baloon)
                }
                else if (nam == "boat") {
                    mDialog.img2.setImageResource(R.drawable.boat)
                }
                else if (nam == "door") {
                    mDialog.img2.setImageResource(R.drawable.door)
                }
                else if (nam == "egg") {
                    mDialog.img2.setImageResource(R.drawable.egg)
                }
                else if (nam == "arrow") {
                    mDialog.img2.setImageResource(R.drawable.arrow)
                }
                else  if (nam == "audi") {
                    mDialog.img2.setImageResource(R.drawable.audi)
                }
                else  if (nam == "love") {
                    mDialog.img2.setImageResource(R.drawable.ic_baseline_favorite_24)
                }
                else  if (nam == "nike") {
                    mDialog.img2.setImageResource(R.drawable.nike)
                }
                else if (nam == "rocket") {
                    mDialog.img2.setImageResource(R.drawable.rocket)
                }
                else  if (nam.equals("gun")) {
                    mDialog.img2.setImageResource(R.drawable.gun)
                }

            }


            else if (i == third) {
                mDialog.word3.text = wordsCollection[2]
                var na = wordsCollection[2]
                if(na.equals("bat")){
                    mDialog.img3.setImageResource(R.drawable.bat)
                }
                else if(na.equals("ball")){
                    mDialog.img3.setImageResource(R.drawable.ball)
                }
               else if(na.equals("flag")){
                    mDialog.img3.setImageResource(R.drawable.flag)
                }
                else if(na == "apple"){
                    mDialog.img3.setImageResource(R.drawable.apple)
                }
                else  if(na == "baloon"){
                    mDialog.img3.setImageResource(R.drawable.baloon)
                }
                else if(na == "boat"){
                    mDialog.img3.setImageResource(R.drawable.boat)
                }
                else  if(na == "door"){
                    mDialog.img3.setImageResource(R.drawable.door)
                }
                else if(na== "egg"){
                    mDialog.img3.setImageResource(R.drawable.egg)
                }
                else  if(na== "arrow"){
                    mDialog.img3.setImageResource(R.drawable.arrow)
                }
                else  if(na == "audi"){
                    mDialog.img3.setImageResource(R.drawable.audi)
                }
                else if(na == "love"){
                    mDialog.img3.setImageResource(R.drawable.ic_baseline_favorite_24)
                }
                else if(na == "nike"){
                    mDialog.img3.setImageResource(R.drawable.nike)
                }
                else if(na == "rocket"){
                    mDialog.img3.setImageResource(R.drawable.rocket)
                }
                else if(na.equals("gun")){
                    mDialog.img3.setImageResource(R.drawable.gun)
                }

            }
            else break
        }

        val window = mDialog.window
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mDialog.setCanceledOnTouchOutside(false) // prevent dialog box from getting dismissed on outside touch
        mDialog.setCancelable(false)  //prevent dialog box from getting dismissed on back key pressed
        mDialog.show()

        mDialog.word1.setOnClickListener {
            choseWordUploadToDatabase(mDialog.word1.text.toString())
            mDialog.dismiss()
        }
        mDialog.word2.setOnClickListener {
            choseWordUploadToDatabase(mDialog.word2.text.toString())
            mDialog.dismiss()
        }
        mDialog.word3.setOnClickListener {
            choseWordUploadToDatabase(mDialog.word3.text.toString())
            mDialog.dismiss()
        }
    }


    /** The next player to get the chance is decided through here. */
    private fun changeUserChance() {
        database.child(getString(R.string.rooms)).child(reference.toString())
            .child(getString(R.string.info)).child(getString(R.string.chanceChange))
            .setValue(0)

        indexOfChance++
        Log.i("TIMER", "index $indexOfChance")
        Log.i("TIMER", "listsize ${playersList.size}")
        if (indexOfChance >= playersList.size) {
            //changeCurrRound()
            roundTillNow++
            indexOfChance = 0
            Log.i("TIMER", "new index $indexOfChance")
            Log.i("TIMER", "round $roundTillNow")
            /** child of info created named as current round */
            database.child(getString(R.string.rooms)).child(reference.toString())
                .child(getString(R.string.info)).child(getString(R.string.currentRound))
                .setValue(roundTillNow + 1)

        }
        hostUID = playersList[indexOfChance].UID!!
        numGuesPlayer = 0
        paintView.clear()
        postRef.push().setValue(Information(10001f, 10001f, 3))
        paintView.isclear = 1
        if (roundTillNow < noOfRounds) {
            database.child(getString(R.string.rooms)).child(reference.toString())
                .child(getString(R.string.info)).child(getString(R.string.chanceUID))
                .setValue(playersList[indexOfChance].UID)
            Log.i("TIMER", "UID$roundTillNow - $noOfRounds")
            Log.i("TIMER", timeLimit.toString())
        } else {
            gameOverUploadToDatabase()
        }
    }

    /** displaying the current round*/
    private fun changeCurrentRoundListener() {

        RoundChangeRef = database.child(getString(R.string.rooms)).child(reference.toString())
            .child(getString(R.string.info)).child(getString(R.string.currentRound))

        valueEventListenerForRoundChange = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value != null)
                //Updating the value of current round
                    rounds_left.text = snapshot.value.toString()
                if (snapshot.value.toString() == (noOfRounds + 1).toString()) {
                    scoreBoard()
                    booleanForGameEnd=true;
                }
            }
        }
        RoundChangeRef.addValueEventListener(valueEventListenerForRoundChange)
    }

    /** Countdown timer for a round. */
    private fun countdown(sec: Long) {
        countdownTimer = object : CountDownTimer(sec, 1000) {

            override fun onFinish() {
                timer_xml.text = " "
                word_xml.text = " "
                stringDisplay = ""
                flagtime = false
                if (serverHost == 1) {
                    database.child(getString(R.string.rooms)).child(reference.toString())
                        .child(getString(R.string.info))
                        .child(getString(R.string.chanceChange)).setValue(1)
                    correctWordUploadToDatabase(guessingWord)
                }
                Log.i("TIMER", "Finish")
            }

            override fun onTick(p0: Long) {

                if (p0 <= (timeLimit / 2) && !flagtime && host == 0) {
                    flagtime = true
                    val random = Random.nextInt(0, guessingWord.length - 1)
                    Log.i("Random", random.toString())

                    stringDisplay = ""
                    for (i in guessingWord.indices) {
                        if (i != random)
                            stringDisplay += "_"
                        else
                            stringDisplay += guessingWord[random]
                    }
                    word_xml.text = stringDisplay
                }
                timer_xml.text = (p0 / 1000).toString()
            }
        }
        countdownTimer.start()
        booleanForCountdownStartedOrNot = true
    }

    /** Chat messages upload to Firebase Database */
    private fun chatUploadToDatabase(cur_text: String) {
        val textObj = ChatText(userId, userName, cur_text)
        postReference.push().setValue(textObj)
    }

    /** Correct Word upload to Firebase Database */
    private fun correctWordUploadToDatabase(cur_text: String) {
        val textObj = ChatText(userId, getString(R.string.correct_word), cur_text)
        postReference.push().setValue(textObj)
    }

    /** Game Over upload to Firebase Database */
    private fun gameOverUploadToDatabase() {
        val textObj = ChatText(userId, getString(R.string.game), getString(R.string.over))
        postReference.push().setValue(textObj)
    }

    /** Chosen word upload to Firebase Database */
    private fun choseWordUploadToDatabase(word: String) {
        Toast.makeText(this, word, Toast.LENGTH_SHORT).show()
        database.child(getString(R.string.rooms)).child(reference.toString())
            .child(getString(R.string.info)).child(getString(R.string.wordToGuess))
            .setValue(word)
    }

    override fun onPause() {
        super.onPause()
        Log.i("GAME ACTIVITY", "ON PAUSE");
        if (backButtonPressedBoolean) {
            deleteCurrentPlayer()
            deleteCurrentRoomIfNoOtherPlayerRemains()
            if (booleanForCountdownStartedOrNot) {
                countdownTimer.cancel()
                booleanForCountdownStartedOrNot = false
            }
            booleanForCountdownCancelled = true
        }

        //called when user cancel/exit the application
        if (!goToMainActivityBoolean) {
            deleteCurrentPlayer()
            deleteCurrentRoomIfNoOtherPlayerRemains()
            if (booleanForCountdownStartedOrNot) {
                countdownTimer.cancel()
                booleanForCountdownStartedOrNot = false
            }
            booleanForCountdownCancelled = true
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("GAME ACTIVITY", "ON RESUME");
        backButtonPressedBoolean = false
        goToMainActivityBoolean = false
        checkRoomExistOrNot()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        backButtonPressedBoolean = true
        routeToMainActivity()
    }

    /** Route to [MainActivity]. */
    private fun routeToMainActivity() {
        goToMainActivityBoolean = true
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAndRemoveTask()
    }

    /** Deletion of current Player from the Room. */
    private fun deleteCurrentPlayer() {
        val userId: String? = prefs.getString(getString(R.string.userId), getString(R.string.EMPTY))
        if (userId != getString(R.string.EMPTY)) {
            database.child(getString(R.string.rooms)).child(reference.toString())
                .child(getString(R.string.Players)).child(userId!!)
                .removeValue()
        }
    }

    /** Delete Room and corresponding data if no Player remains. */
    private fun deleteCurrentRoomIfNoOtherPlayerRemains() {
        if (playerCount <= baseCount) {
            database.child(getString(R.string.rooms)).child(reference.toString()).removeValue()
            database.child(getString(R.string.games)).child(reference.toString()).removeValue()
            database.child(getString(R.string.drawingData)).child(reference.toString())
                .removeValue()
        }
    }

    /** Check whether Rooms exist or not while Joining.
     * If exist, add the Player to the Room.
     * Else, redirect Player to [MainActivity]*/
    private fun checkRoomExistOrNot() {
        // room reference
        val rootRef =
            database.child(getString(R.string.rooms)).child(reference.toString())

        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userId: String? =
                        prefs.getString(getString(R.string.userId), getString(R.string.EMPTY))
                    val userName: String? =
                        prefs.getString(getString(R.string.userName), getString(R.string.EMPTY))
                    if (userId != getString(R.string.EMPTY)) {
                        Log.i("###WAITINGACTIVITY", "done")
                        database.child(getString(R.string.rooms)).child(reference.toString())
                            .child(getString(R.string.Players))
                            .child(userId.toString()).setValue(playerInfo(userName, 0, userId))
                    }
                } else {
                    routeToMainActivity()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}

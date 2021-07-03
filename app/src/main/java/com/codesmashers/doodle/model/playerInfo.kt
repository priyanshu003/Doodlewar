package com.codesmashers.doodle.model
import com.codesmashers.doodle.WaitingActivity
import com.codesmashers.doodle.GameActivity

/**
 * Stores the information about each player joined in a Room to be populated in [WaitingActivity] and [GameActivity].
 *
 * @param Name: Name of the Player.
 * @param score: Score obtained by a player in a Game.
 * @param UID: UniqueID of the Player.
 */

class playerInfo(
    var Name: String? = "",
    var score: Int = 0,
    var UID: String? = ""
)

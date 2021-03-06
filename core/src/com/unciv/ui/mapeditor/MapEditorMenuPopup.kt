package com.unciv.ui.mapeditor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Json
import com.unciv.Constants
import com.unciv.MainMenuScreen
import com.unciv.UncivGame
import com.unciv.logic.MapSaver
import com.unciv.logic.map.MapType
import com.unciv.logic.map.RoadStatus
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.Player
import com.unciv.ui.saves.Gzip
import com.unciv.ui.utils.*
import com.unciv.ui.worldscreen.mainmenu.DropBox
import kotlin.concurrent.thread

class MapEditorMenuPopup(var mapEditorScreen: MapEditorScreen): Popup(mapEditorScreen){
    private val mapNameEditor: TextField = TextField(mapEditorScreen.mapName, skin)

    init {
        mapNameEditor.textFieldFilter = TextField.TextFieldFilter { _, char -> char != '\\' && char != '/' }
        add(mapNameEditor).fillX().row()
        mapNameEditor.selectAll()
        mapNameEditor.maxLength = 240       // A few under max for most filesystems
        mapEditorScreen.stage.keyboardFocus = mapNameEditor

        addNewMapButton()
        addSaveMapButton()
        addCopyMapAsTextButton()
        addLoadMapButton()
        addExitMapEditorButton()
        addCloseOptionsButton()
    }

    private fun Popup.addNewMapButton() {
        val newMapButton = "New map".toTextButton()
        newMapButton.onClick {
            UncivGame.Current.setScreen(NewMapScreen())
        }
        add(newMapButton).row()
    }

    private fun Popup.addSaveMapButton() {
        val saveMapButton = "Save map".toTextButton()
        saveMapButton.onClick {
            mapEditorScreen.tileMap.mapParameters.name = mapEditorScreen.mapName
            mapEditorScreen.tileMap.mapParameters.type = MapType.custom
            thread(name = "SaveMap") {
                try {
                    MapSaver.saveMap(mapEditorScreen.mapName, mapEditorScreen.tileMap)

                    close()
                    Gdx.app.postRunnable {
                        ToastPopup("Map saved", mapEditorScreen) // todo - add this text to translations
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Gdx.app.postRunnable {
                        val cantLoadGamePopup = Popup(mapEditorScreen)
                        cantLoadGamePopup.addGoodSizedLabel("It looks like your map can't be saved!").row()
                        cantLoadGamePopup.addCloseButton()
                        cantLoadGamePopup.open(force = true)
                    }
                }
            }
        }
        saveMapButton.isEnabled = mapNameEditor.text.isNotEmpty()
        add(saveMapButton).row()
        mapNameEditor.addListener {
            mapEditorScreen.mapName = mapNameEditor.text
            saveMapButton.isEnabled = mapNameEditor.text.isNotEmpty()
            true
        }
    }

    private fun Popup.addCopyMapAsTextButton() {
        val copyMapAsTextButton = "Copy to clipboard".toTextButton()
        copyMapAsTextButton.onClick {
            val json = Json().toJson(mapEditorScreen.tileMap)
            val base64Gzip = Gzip.zip(json)
            Gdx.app.clipboard.contents = base64Gzip
        }
        add(copyMapAsTextButton).row()
    }

    private fun Popup.addLoadMapButton() {
        val loadMapButton = "Load map".toTextButton()
        loadMapButton.onClick {
            UncivGame.Current.setScreen(LoadMapScreen(mapEditorScreen.tileMap))
        }
        add(loadMapButton).row()
    }


    private fun Popup.addExitMapEditorButton() {
        val exitMapEditorButton = "Exit map editor".toTextButton()
        add(exitMapEditorButton).row()
        exitMapEditorButton.onClick { mapEditorScreen.game.setScreen(MainMenuScreen()); mapEditorScreen.dispose() }
    }

    private fun Popup.addCloseOptionsButton() {
        val closeOptionsButton = Constants.close.toTextButton()
        closeOptionsButton.onClick { close() }
        add(closeOptionsButton).row()
    }

    private fun getPlayersFromMap(tileMap: TileMap): ArrayList<Player> {
        val tilesWithStartingLocations = tileMap.values
                .filter { it.improvement != null && it.improvement!!.startsWith("StartingLocation ") }
        var players = ArrayList<Player>()
        for (tile in tilesWithStartingLocations) {
            players.add(Player().apply{
                chosenCiv = tile.improvement!!.removePrefix("StartingLocation ")
            })
        }
        return players
    }

}

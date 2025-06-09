package co.candyhouse.app.tabs.devices.hub3.bean

import candyhouse.sesameos.ir.base.IrRemote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 红外线设备
 *
 * @author frey on 2025/1/9
 */

data class IrRemoteState(
    val remoteMap: HashMap<String, ArrayList<IrRemote>> = HashMap()
)

class IrRemoteRepository {
    private val _state = MutableStateFlow(IrRemoteState())
    val state = _state.asStateFlow()

    fun addRemote(key: String, remote: IrRemote) {
        val currentMap = _state.value.remoteMap.toMutableMap()
        val list = currentMap[key]?.toMutableList() ?: mutableListOf()
        list.add(remote)
        currentMap[key] = ArrayList(list)
        _state.value = IrRemoteState(HashMap(currentMap))
    }

    fun setRemotes(key: String, remotes: List<IrRemote>) {
        val currentMap = _state.value.remoteMap.toMutableMap()
        currentMap[key] = ArrayList(remotes)
        _state.value = IrRemoteState(HashMap(currentMap))
    }

    fun addRemotes(key: String, remotes: List<IrRemote>) {
        val currentMap = _state.value.remoteMap.toMutableMap()
        val existingList = currentMap[key]?.toMutableList() ?: mutableListOf()
        existingList.addAll(remotes)
        currentMap[key] = ArrayList(existingList)
        _state.value = IrRemoteState(HashMap(currentMap))
    }

    fun removeRemote(key: String, remote: IrRemote) {
        val currentMap = _state.value.remoteMap.toMutableMap()
        currentMap[key]?.let { list ->
            list.remove(remote)
            if (list.isEmpty()) {
                currentMap.remove(key)
            }
        }
        _state.value = IrRemoteState(HashMap(currentMap))
    }

    fun updateRemote(key: String, index: Int, remote: IrRemote) {
        val currentMap = _state.value.remoteMap.toMutableMap()
        currentMap[key]?.let { list ->
            if (index in list.indices) {
                val newList = ArrayList(list)
                newList[index] = remote
                currentMap[key] = newList
                _state.value = IrRemoteState(HashMap(currentMap))
            }
        }
    }

    fun getRemotesByKey(key: String): List<IrRemote> {
        return _state.value.remoteMap[key] ?: emptyList()
    }

    fun getRemoteByIndex(key: String, index: Int): IrRemote? {
        return _state.value.remoteMap[key]?.getOrNull(index)
    }

    fun containsRemote(key: String, remote: IrRemote): Boolean {
        return _state.value.remoteMap[key]?.contains(remote) ?: false
    }

    fun getAllKeys(): Set<String> {
        return _state.value.remoteMap.keys
    }

    fun clearRemotes(key: String) {
        val currentMap = _state.value.remoteMap.toMutableMap()
        currentMap.remove(key)
        _state.value = IrRemoteState(HashMap(currentMap))
    }

    fun clearAll() {
        _state.value = IrRemoteState()
    }

}

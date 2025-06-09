package candyhouse.sesameos.ir.domain.bizAdapter.bizBase


interface IrInterface {
    @Throws(Exception::class)
    fun findType(typeIndex: Int, key: Int): ByteArray
    @Throws(Exception::class)
    fun findBrand(brandIndex: Int, key: Int): ByteArray
    @Throws(Exception::class)
    fun search(arrayIndex: Int): UByteArray
    fun getStudyData(data: ByteArray, len: Int): ByteArray
    fun getBrandArray(brandIndex: Int): IntArray?
    fun getTypeArray(typeIndex: Int): IntArray?
    fun getTypeCount(typeIndex: Int): Int
    fun getBrandCount(brandIndex: Int): Int
    fun getTableCount(): Int

}
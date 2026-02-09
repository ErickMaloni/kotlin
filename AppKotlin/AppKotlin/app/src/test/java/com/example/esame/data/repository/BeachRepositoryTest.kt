package com.example.esame.data.repository

import com.example.esame.data.model.Umbrella
import org.junit.Assert
import org.junit.Test

class BeachRepositoryTest {

    @Test
    fun `ordinamento ombrelloni deve posizionare correttamente le file`() {
        // 1. Dati di test disordinati
        val u1 = Umbrella(id = "A", number = 1, rowIndex = 2)
        val u2 = Umbrella(id = "B", number = 2, rowIndex = 0)
        val u3 = Umbrella(id = "C", number = 3, rowIndex = 1)
        val listDisordinata = listOf(u1, u2, u3)

        // 2. Azione: applichiamo l'ordinamento
        val listOrdinata = listDisordinata.sortedBy { it.rowIndex }

        // 3. Verifica: controlliamo che l'ordine sia corretto
        Assert.assertEquals("Il primo elemento dovrebbe avere rowIndex 0", 0L, listOrdinata[0].rowIndex)
        Assert.assertEquals("Il secondo elemento dovrebbe avere rowIndex 1", 1L, listOrdinata[1].rowIndex)
        Assert.assertEquals("Il terzo elemento dovrebbe avere rowIndex 2", 2L, listOrdinata[2].rowIndex)
    }
}

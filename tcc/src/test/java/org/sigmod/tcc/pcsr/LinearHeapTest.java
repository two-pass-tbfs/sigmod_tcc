package org.sigmod.tcc.pcsr;

import junit.framework.TestCase;
import org.sigmod.tcc.clique.sparse.LinearHeap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LinearHeapTest extends TestCase {

    public void testLinearHeap() {
        LinearHeap heap = new LinearHeap(4, 5);
//        int[] ids = {0, 1, 2, 3};
        ArrayList<Integer> ids = new ArrayList<>(List.of(0, 1, 2, 3));
        int[] keys = {5, 5, 4, 3};


        System.out.println("isEmpty? " + heap.isEmpty());
        heap.init(4, 5, 0, ids, keys, true);
        heap.debugPrint();

        System.out.println("------ Testing compare ------");
        System.out.println("compare(0, 1): " + heap.compare(1, 2));
        System.out.println("compare(0, 2): " + heap.compare(3, 1));

        System.out.println("-------Testing iterator-------");
        for (LinearHeap.Element pair : heap) {
            System.out.println("ID: " + pair.id() + " Key: " + pair.key());
        }
        System.out.println();

        System.out.println("-------Testing iterator at key 5-------");
        Iterator<LinearHeap.Element> ascIt = heap.iterator(5);
        while (ascIt.hasNext()) {
            LinearHeap.Element pair = ascIt.next();
            System.out.println("ID: " + pair.id() + " Key: " + pair.key());
        }
        System.out.println();

        System.out.println("-------Testing iterator at Pair(2, 4)-------");
        LinearHeap.Element startElement = new LinearHeap.Element(2, 4);
        ascIt = heap.iterator(startElement);
        while (ascIt.hasNext()) {
            LinearHeap.Element pair = ascIt.next();
            System.out.println("ID: " + pair.id() + " Key: " + pair.key());
        }
        System.out.println();

        System.out.println("-------Testing descending iterator-------");
        Iterator<LinearHeap.Element> descIt = heap.descendingIterator();
        while (descIt.hasNext()) {
            LinearHeap.Element pair = descIt.next();
            System.out.println("ID: " + pair.id() + " Key: " + pair.key());
        }
        System.out.println();

        System.out.println("-------Testing descending iterator at key 4-------");
        descIt = heap.descendingIterator(4);
        while (descIt.hasNext()) {
            LinearHeap.Element pair = descIt.next();
            System.out.println("ID: " + pair.id() + " Key: " + pair.key());
        }
        System.out.println();

        System.out.println("-------Testing descending iterator at Pair(2, 4)-------");
        startElement = new LinearHeap.Element(2, 4);
        descIt = heap.descendingIterator(startElement);
        while (descIt.hasNext()) {
            LinearHeap.Element pair = descIt.next();
            System.out.println("ID: " + pair.id() + " Key: " + pair.key());
        }

        System.out.println("--------------Testing popMin---------------- ");
        System.out.println("getMin: " + heap.getMin());
        System.out.println("popMin: " + heap.popMin());
        heap.debugPrint();
        System.out.println();

        System.out.println("--------------Testing decrement---------------- ");
        System.out.println("Trying out decrement: ");
        heap.decrement(2, 2);
        heap.debugPrint();

        System.out.println("--------------Testing popMax---------------------");
        System.out.println("Popped: " + heap.popMax());
        heap.debugPrint();

        System.out.println("--------------Testing popMax---------------------");
        System.out.println("isEmpty? " + heap.isEmpty());
        while (!heap.isEmpty()) {
            System.out.println("Popped: " + heap.popMax());
            System.out.println("isEmpty? " + heap.isEmpty());
            heap.debugPrint();
        }
        System.out.println("isEmpty? " + heap.isEmpty());
        System.out.printf("Size: %d%n", heap.size());
    }
}

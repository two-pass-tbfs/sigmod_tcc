package org.sigmod.tcc.clique.sparse;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Constructs a Linear Heap.
 */
public class LinearHeap implements Iterable<LinearHeap.Element>, Comparator<Integer> {

    public record Element(Integer id, Integer key) {
    }

    private int n; // number of vertices
    private int keyCap; // the maximum allowed key value
    // this also determines the size of heads
    private int minKey; // possible minimum key
    private int maxKey; // possible maximum key
    // this is used to iterate through all elements

    private int[] keys; // key of vertices
    // this indicates the priority of each vertex ID. here, the degrees or tdc of the vertices.
    private int[] heads; // head of a doubly linked list
    // this stores the ID of the first element in the list of elements that have a key k
    // if the heads[k] == n, it means the list with a key k is empty
    private int[] tails;
    private int[] prevs; // pre for doubly-linked list
    // if the prevs[id] == n, the id is the heads of its list
    private int[] nexts; // next for a doubly linked list
    // if the nexts[id] == n, the id is the tail of the list

    private int[] orders;

    public static final Logger log = Logger.getLogger(LinearHeap.class.getName());

    /**
     *
     * @param n      the number of vertices
     * @param keyCap the maximum allowed key
     */
    public LinearHeap(int n, int keyCap) {
        this.n = n;
        this.keyCap = keyCap;
        this.minKey = this.maxKey = this.keyCap;

        log.fine(() -> String.format("LinearHeap initialized with n: %d, keyCap: %d, minKey: %d, maxKey: %d", this.n, this.keyCap, this.minKey, this.maxKey));
    }

    public void debugPrint() {
        System.out.println("Min Key: " + this.minKey + " maxKey: " + this.maxKey);
        for (int i = this.minKey; i <= this.maxKey; i++) {
            if (this.heads[i] != this.n) {
                System.out.println("Head of the list with key " + i + ": " + this.heads[i]);
                int curr = this.heads[i];
                while (curr != this.n) {
                    System.out.println("ID: " + curr);
                    curr = this.nexts[curr];
                }
                System.out.println("---------------------------------------");
            }
        }
    }

    public boolean isEmpty() {
        if (this.heads == null) return true;

        return getMin() == null;
    }

    public int size() {
        if (this.heads == null) return 0;
        return this.n;
    }

    /**
     * Initializes the heap
     *
     * @param n         the size of the array
     * @param keyCap    the maximum allowed key
     * @param offset    the starting index
     * @param ids       the reference vertex id list
     * @param keys      the values associated with the ids (degree or tdc)
     * @param initOrder if true, the order of the ids will be initialized, otherwise it will be left uninitialized.
     */
    public void init(int n, int keyCap, int offset, List<Integer> ids, int[] keys, boolean initOrder) {
        if (this.keys == null) this.keys = new int[this.n];
        if (this.prevs == null) this.prevs = new int[this.n];
        if (this.nexts == null) this.nexts = new int[this.n];
        if (this.heads == null) this.heads = new int[this.keyCap + 1];
        if (this.tails == null) this.tails = new int[this.keyCap + 1];

        assert keyCap <= this.keyCap;
        this.minKey = this.maxKey = keyCap;
        for (int i = 0; i <= keyCap; i++) {
            this.heads[i] = this.n; // Initialize the heads of from 0 to keyCap to n, meaning the lists with key k are empty
            this.tails[i] = this.n;
        }

        for (int i = 0; i < n; i++) {
            int id = ids.get(i + offset);

            int key = keys[id]; // degree of vertex ID
            assert id < this.n;
            assert key <= keyCap;
            this.keys[id] = key;
            this.prevs[id] = this.n;
            this.nexts[id] = this.heads[key];
            if (this.heads[key] != this.n)
                this.prevs[this.heads[key]] = id;
            else this.tails[key] = id;
            this.heads[key] = id;

            if (key < minKey)
                minKey = key;
        }

        if (initOrder) {
            if (this.orders == null) this.orders = new int[this.n];
            Arrays.fill(this.orders, -1);
            int orderIndex = 0;
            for (Element p : this) {
                this.orders[p.id] = orderIndex++;
            }
        }
    }

    public int getKey(int id) {
        return this.keys[id];
    }

    public int[] getKeys() {
        return this.keys;
    }

    public int getIds(int offset, List<Integer> vs, int vsSize) {
        for (int i = this.minKey; i <= this.maxKey; i++) {
            for (int id = this.heads[i]; id != this.n; id = this.nexts[id]) {
//                vs[offset + vsSize++] = id;
                vs.set(offset + vsSize++, id);
            }
        }
        return vsSize;
    }

    public Element getMin() {
        while (this.minKey <= this.maxKey && this.heads[this.minKey] == this.n)
            ++this.minKey;

        if (this.minKey > this.maxKey)
            return null;


        return new Element(this.heads[this.minKey], this.minKey);
    }

    public Element popMin() {
        while (this.minKey <= this.maxKey && this.heads[this.minKey] == this.n)
            ++this.minKey;

        if (this.minKey > this.maxKey) {
            return null;
        }
        int id = this.heads[this.minKey];
        Element output = new Element(id, this.minKey);

        this.heads[this.minKey] = this.nexts[id];
        if (this.heads[this.minKey] != this.n)
            this.prevs[this.heads[this.minKey]] = this.n;
        else this.tails[this.minKey] = this.n;

        return output;
    }

    public Element popMax() {
        while (this.maxKey >= this.minKey && this.heads[this.maxKey] == this.n)
            --this.maxKey;

        if (this.maxKey < this.minKey) {
            return null;
        }

        int id = this.tails[this.maxKey];
        if (id == this.n) {
            return null;
        }
        Element output = new Element(id, this.maxKey);

        // Remove the tail
        if (this.prevs[id] != this.n) {
            this.nexts[this.prevs[id]] = this.n; // If the tail has a prev element, update its next pointer to n
            this.tails[this.maxKey] = this.prevs[id]; // the prev element becomes the new element
        } else { // the tail was also the head
            this.heads[this.maxKey] = this.n;
            this.tails[this.maxKey] = this.n;
        }

        // update maxKey if the list becomes empty
        if (this.heads[this.maxKey] == this.n) {
            while (this.maxKey >= this.minKey && this.heads[this.maxKey] == this.n)
                --this.maxKey;

            if (this.maxKey < this.minKey)
                this.minKey = this.keyCap;
        }

        return output;
    }

    public int decrement(int id, int value) {
//        if (this.keys[id] < value) {
//            log.warning(String.format("ID: %d, Key %d is smaller than value %d, returning without changing anything",id, this.keys[id], value));
//            return -1;
//        }

        int currentKey = this.keys[id];
        // Step 1: Remove id from its current list
        if (this.prevs[id] == this.n) { // Case 1: id is the head of its list
            this.heads[currentKey] = this.nexts[id];

            if (this.nexts[id] != this.n)
                this.prevs[this.nexts[id]] = this.n;
            else this.tails[currentKey] = this.n;
        } else { // Case 2: id is not the head
            int pid = this.prevs[id];
            this.nexts[pid] = this.nexts[id];

            if (this.nexts[id] != this.n)
                this.prevs[this.nexts[id]] = pid;
            else this.tails[currentKey] = pid;
        }

        // Step 2: If the list at currentKey became empty after removal and the currentKey was the maxKey, find the new maxKey
        if (currentKey == this.maxKey && this.heads[currentKey] == this.n) {
            while (this.maxKey >= this.minKey && this.heads[this.maxKey] == this.n)
                --this.maxKey;
        }

        // Step 3: Decrement the key
        this.keys[id] -= value;
        int newKey = this.keys[id];

        // Step 4: Insert id into the new list
        this.prevs[id] = this.n;
        this.nexts[id] = this.heads[newKey];

        if (this.heads[newKey] != this.n)
            this.prevs[this.heads[newKey]] = id;
        else this.tails[newKey] = id;
        this.heads[newKey] = id;


        // Step 5: Update minKey or maxKey if necessary
        if (newKey < this.minKey)
            this.minKey = newKey;

        if (newKey > this.maxKey)
            this.maxKey = newKey;

        return newKey;
    }

    // ----- Iterator functions -----
    @Override
    public Iterator<Element> iterator() {
        return new LinearHeapIterator(this, this.minKey);
    }

    public Iterator<Element> iterator(int startKey) {
        return new LinearHeapIterator(this, startKey);
    }

    public Iterator<Element> iterator(Element startElement) {
        return new LinearHeapIterator(this, startElement);
    }

    @Override
    public void forEach(Consumer<? super Element> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<Element> spliterator() {
        return Iterable.super.spliterator();
    }

    // ----- Descending Iterator -----
    public Iterator<Element> descendingIterator() {
        return new LinearHeapDescendingIterator(this, this.keyCap);
    }

    public Iterator<Element> descendingIterator(int startKey) {
        return new LinearHeapDescendingIterator(this, startKey);
    }

    public Iterator<Element> descendingIterator(Element startElement) {
        return new LinearHeapDescendingIterator(this, startElement);
    }

    @Override
    public int compare(Integer id1, Integer id2) {
        if (this.orders == null)
            throw new IllegalStateException("Heap has no order information");

        if (Objects.equals(id1, id2)) {
            return 0;
        }

        if (id1 == null || id1 < 0 || id1 >= this.orders.length ||
                id2 == null || id2 < 0 || id2 >= this.orders.length) {
            throw new IllegalArgumentException(String.format("ID out of bounds. id1: %s, id2: %s", id1, id2));
        }

        int order1 = this.orders[id1];
        int order2 = this.orders[id2];

        if (order1 != -1 && order2 != -1) {
            return Integer.compare(order1, order2);
        }

        throw new IllegalStateException(String.format("Id not in heap. Order of id1: %d and id2: %d", order1, order2));
    }

    // ----- Iterator implementation -----
    private class LinearHeapIterator implements Iterator<Element> {
        final LinearHeap heap;
        int currentKey;
        int currentId;
        Element peekedElement;

        public LinearHeapIterator(LinearHeap heap, int startKey) {
            this.heap = heap;
            this.currentKey = Math.max(startKey, this.heap.minKey);
            this.currentId = this.heap.heads[this.currentKey];

            if (this.currentId == this.heap.n || this.currentKey > this.heap.maxKey) {
                while (this.currentKey <= this.heap.maxKey && this.heap.heads[this.currentKey] == this.heap.n)
                    this.currentKey++;

                if (this.currentKey <= this.heap.maxKey)
                    this.currentId = this.heap.heads[this.currentKey];
                else this.currentId = this.heap.n;
            }

            if (this.currentId != this.heap.n)
                this.peekedElement = new Element(this.currentId, this.heap.keys[this.currentId]);
            else this.peekedElement = null;
        }

        public LinearHeapIterator(LinearHeap heap, Element startElement) {
            this.heap = heap;
            this.currentKey = startElement.key;
            this.currentId = this.heap.n;
            this.peekedElement = null;

            if (this.currentKey < this.heap.minKey || this.currentKey > this.heap.maxKey || this.currentKey < 0 || this.currentKey > this.heap.keyCap) {
                return;
            }

            int tempId = this.heap.heads[this.currentKey];
            while (tempId != this.heap.n) {
                if (tempId == startElement.id) {
                    this.currentId = tempId;
                    this.peekedElement = new Element(this.currentId, this.heap.keys[this.currentId]);
                    break;

                }
                tempId = this.heap.nexts[tempId];
            }
        }

        private void findNextElement() {
            // Try to move the next ID in the current linked list
            currentId = heap.nexts[currentId];

            while (currentId == heap.n) {
                currentKey++;
                if (currentKey > heap.maxKey) {
                    this.peekedElement = null;
                    return;
                }
                currentId = heap.heads[currentKey];
            }
            peekedElement = new Element(currentId, heap.keys[currentId]);
        }

        @Override
        public boolean hasNext() {
            return peekedElement != null;
        }

        @Override
        public Element next() {
            if (!hasNext())
                throw new NoSuchElementException();

            Element output = peekedElement;
            findNextElement();
            return output;
        }
    }

    private class LinearHeapDescendingIterator implements Iterator<Element> {
        final LinearHeap heap;
        int currentKey;
        int currentId;
        Element peekedElement;

        public LinearHeapDescendingIterator(LinearHeap heap, int startKey) {
            this.heap = heap;
            this.currentKey = Math.min(startKey, this.heap.maxKey);

            if (this.currentKey < this.heap.minKey && this.heap.maxKey >= this.heap.minKey) {
                log.warning(() -> String.format("Start key %d is smaller than minimum key %d. Iterating from the maximum key instead.", this.currentKey, this.heap.minKey));
                this.currentKey = this.heap.maxKey;
            } else if (this.heap.maxKey < this.heap.minKey) { // Heap is empty
                this.currentKey = -1;
            }

            if (this.currentKey >= 0 && this.currentKey <= this.heap.keyCap) {
                this.currentId = this.heap.tails[this.currentKey];
            } else {
                this.currentId = this.heap.n;
            }

            while (this.currentKey >= this.heap.minKey && this.currentId == this.heap.n) {
                this.currentKey--;
                if (this.currentKey < this.heap.minKey) {
                    this.currentId = this.heap.n;
                    break;
                }
                this.currentId = this.heap.tails[this.currentKey];
            }

            if (this.currentId != this.heap.n)
                this.peekedElement = new Element(this.currentId, this.heap.keys[this.currentId]);
            else this.peekedElement = null;
        }

        public LinearHeapDescendingIterator(LinearHeap heap, Element startElement) {
            this.heap = heap;
            this.currentKey = startElement.key;
            this.currentId = startElement.id;
            this.peekedElement = new Element(this.currentId, this.heap.keys[this.currentId]);

            if (this.currentKey < this.heap.minKey || this.currentKey > this.heap.maxKey || this.currentKey < 0 || this.currentKey > this.heap.keyCap) {
                return;
            }

            int tempId = this.heap.tails[this.currentKey];
            while (tempId != this.heap.n) {
                if (tempId == startElement.id) {
                    this.currentId = tempId;
                    this.peekedElement = new Element(this.currentId, this.heap.keys[this.currentId]);
                    break;
                }
                tempId = this.heap.prevs[tempId];
            }
        }

        @Override
        public boolean hasNext() {
            return peekedElement != null;
        }

        @Override
        public Element next() {
            if (!hasNext())
                throw new NoSuchElementException();

            Element output = peekedElement;
            findPreviousElement();
            return output;
        }

        private void findPreviousElement() {
            this.currentId = this.heap.prevs[currentId];
            while (this.currentKey >= this.heap.minKey && this.currentId == this.heap.n) {
                this.currentKey--;
                if (this.currentKey < this.heap.minKey) {
                    this.peekedElement = null;
                    return;
                }
                this.currentId = this.heap.tails[this.currentKey];
            }

            if (this.currentId != this.heap.n)
                this.peekedElement = new Element(this.currentId, this.heap.keys[this.currentId]);
            else this.peekedElement = null;
        }
    }
}

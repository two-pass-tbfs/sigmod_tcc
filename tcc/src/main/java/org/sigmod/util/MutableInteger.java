package org.sigmod.util;

/**
 * A mutable integer class.
 */
public class MutableInteger {
    public int value;

    public MutableInteger(int value) {
        this.value = value;
    }

    // You can add helper methods if you like
    public void increment() {
        this.value++;
    }

    public int get() {
        return this.value;
    }

    public int getAndIncrement() {
        int oldValue = this.value;
        this.value++;
        return oldValue;
    }

    public int getAndDecrement() {
        int oldValue = this.value;
        this.value--;
        return oldValue;
    }

    public int incrementAndGet() {
        this.value++;
        return this.value;
    }

    public int decrementAndGet() {
        this.value--;
        return this.value;
    }

    public void set(int i) {
        this.value = i;
    }

    public int addAndGet(int size) {
        this.value += size;
        return this.value;
    }
}

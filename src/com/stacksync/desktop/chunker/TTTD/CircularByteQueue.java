/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.chunker.TTTD;

/**
 * A fast but unsafe circular byte queue.
 * 
 * There is no enforcement that the indices are valid, and it is easily possible
 * to overflow when adding or polling. But, this is faster than Queue<Byte> by a
 * factor of 5 or so.
 */
public class CircularByteQueue {
	private int size = 0;
	private int head = -1;
	private int tail = 0;
	private final int capacity;
	private final byte[] bytes;

	public CircularByteQueue(int capacity) {
		this.capacity = capacity;
		this.bytes = new byte[capacity];
	}

	/**
	 * Adds the byte to the queue
	 */
	public void add(byte b) {
		head++;
		head %= capacity;
        bytes[head] = b;
		size++;
	}

	/**
	 * Removes and returns the next byte in the queue
	 */
	public byte poll() {
		byte b = bytes[tail];
		tail++;
		tail %= capacity;
		size--;
		return b;
	}

	public boolean isFull() {
		return size >= capacity;
	}
}
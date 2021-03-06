package cp.benchmark.intset;

import java.util.concurrent.locks.*;

/**
 * @author Pascal Felber
 * @author Tiago Vale
 * @since 0.1
 */
public class IntSetLinkedListPerNodeLock implements IntSet {

	public class Node {
		private final int m_value;
		private Node m_next;
		private final Lock l;

		public Node(int value) {
			this(value, null);
		}

		public Node(int value, Node next) {
			m_value = value;
			m_next = next;
			l = new ReentrantLock();
		}

		public int getValue() {
			return m_value;
		}

		public void setNext(Node next) {
			m_next = next;
		}

		public Node getNext() {
			return m_next;
		}

		public void lock() {
			this.l.lock();
		}

		public void unlock() {
			this.l.unlock();
		}
	}

	private final Node m_first;

	public IntSetLinkedListPerNodeLock() {
		m_first = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE));
	}

	public boolean add(int value) {
		boolean result;

		Node previous = m_first;
		previous.lock();
		Node next = previous.getNext();
		next.lock();
		int v;
		try {
			while ((v = next.getValue()) < value) {
				previous.unlock();
				previous = next;
				next = previous.getNext();
				next.lock();
			}
			result = v != value;
			if (result) {
				previous.setNext(new Node(value, next));
			}
			
			return result;
		} finally {
			previous.unlock();
			next.unlock();
		}
	}

	public boolean remove(int value) {
		boolean result;

		Node previous = m_first;
		previous.lock();
		Node next = previous.getNext();
		next.lock();
		int v;
		try {
			while ((v = next.getValue()) < value) {
				previous.unlock();
				previous = next;
				next = previous.getNext();
				next.lock();
			}
			result = v == value;
			if (result) {
				previous.setNext(next.getNext());
			}
			
			return result;
		} finally {
			previous.unlock();
			next.unlock();
		}
	}

	public boolean contains(int value) {
		Node previous = m_first;
		previous.lock();
		Node next = previous.getNext();
		next.lock();
		int v;
		try {
			while ((v = next.getValue()) < value) {
				previous.unlock();
				previous = next;
				next = previous.getNext();
				next.lock();
			}
			
			return (v == value);
		} finally {
			previous.unlock();
			next.unlock();
		}
	}

	public void validate() {
		java.util.Set<Integer> checker = new java.util.HashSet<>();
		int previous_value = m_first.getValue();
		Node node = m_first.getNext();
		int value = node.getValue();
		while (value < Integer.MAX_VALUE) {
			assert previous_value < value : "list is unordered: " + previous_value + " before " + value;
			assert !checker.contains(value) : "list has duplicates: " + value;
			checker.add(value);
			previous_value = value;
			node = node.getNext();
			value = node.getValue();
		}
	}
}

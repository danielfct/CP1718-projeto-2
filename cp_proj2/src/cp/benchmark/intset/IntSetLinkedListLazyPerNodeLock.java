package cp.benchmark.intset;

import java.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.locks.Lock;

/**
 * @author Pascal Felber
 * @author Tiago Vale
 * @since 0.1
 */
public class IntSetLinkedListLazyPerNodeLock implements IntSet {

	public class Node {
		private final int m_value;
		private Node m_next;
		private boolean marked;
		private Lock l;

		public Node(int value, Node next) {
			m_value = value;
			m_next = next;
			marked = false;
			l = new ReentrantLock();
		}

		public Node(int value) {
			this(value, null);
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

		public boolean isMarked() {
			return marked;
		}

		public void setMarked(boolean m) {
			marked = m;
		}

		public void lock() {
			this.l.lock();
		}

		public void unlock() {
			this.l.unlock();
		}
	}

	private final Node m_first;

	public IntSetLinkedListLazyPerNodeLock() {
		m_first = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE));
	}

	public boolean add(int value) {
		boolean result;

		while (true) {
			Node previous = m_first;
			Node next = previous.getNext();
			int v;
			while ((v = next.getValue()) < value) {
				previous = next;
				next = previous.getNext();
			}
			previous.lock();
			next.lock();
			try {
				if (validate(previous, next)) {
					result = v != value;
					if (result) {
						previous.setNext(new Node(value, next));
					}

					return result;
				}
			} finally {
				previous.unlock();
				next.unlock();
			}
		}
	}

	public boolean remove(int value) {
		boolean result;

		while (true) {
			Node previous = m_first;
			Node next = previous.getNext();
			int v;
			while ((v = next.getValue()) < value) {
				previous = next;
				next = previous.getNext();
			}
			previous.lock();
			next.lock();
			try {
				if (validate(previous, next)) {
					result = v == value;
					if (result) {
						next.setMarked(true);
						previous.setNext(next.getNext());
					}
					
					return result;
				}
			} finally {
				previous.unlock();
				next.unlock();
			}
		}
	}

	public boolean contains(int value) {
		Node curr = m_first;
		while (curr.getValue() < value)
			curr = curr.getNext();
		return (curr.getValue() == value && !curr.isMarked());
	}

	private boolean validate(Node previous, Node curr) {
		return !previous.isMarked() && !curr.isMarked() && previous.getNext() == curr;
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

package cp.benchmark.intset;

import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * @author Pascal Felber
 * @author Tiago Vale
 * @since 0.1
 */
public class IntSetLinkedListLockFree implements IntSet {

	public class Node {
		private final int value;
		private AtomicMarkableReference<Node> next;

		public Node(int value, Node next) {
			this.value = value;
			this.next = new AtomicMarkableReference<Node>(next, false);
		}

		public Node(int value) {
			this(value, null);
		}

		public int getValue() {
			return value;
		}

		public boolean compareAndSetNext(Node expectedNode, Node newNode, boolean expectedMark, boolean newMark) {
			return this.next.compareAndSet(expectedNode, newNode, expectedMark, newMark);
		}

		public boolean attemptMarkNext(Node expectedNode, boolean newMark) {
			return this.next.attemptMark(expectedNode, newMark);
		}

		public AtomicMarkableReference<Node> getNext() {
			return next;
		}

	}

	private final Node m_first;

	public IntSetLinkedListLockFree() {
		m_first = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE));
	}

	private Node find(int value) {
		retry: while (true) {
			Node previous = m_first;
			Node current = previous.getNext().getReference();
			while (true) {
				boolean[] marked = { false };
				Node next = current.getNext().get(marked);
				while (marked[0]) {
					if (!previous.compareAndSetNext(current, next, false, false)) 
						continue retry;
					current = next;
					next = current.getNext().get(marked);
				}
				if (current.getValue() >= value)
					return previous;
				previous = current;
				current = next;
			}
		}
	}

	public boolean add(int value) {
		while (true) {
			Node previous = this.find(value);
			Node current = previous.getNext().getReference();
			if (current.getValue() == value) {
				return false;
			}
			if (previous.compareAndSetNext(current, new Node(value, current), false, false)) {
				return true;
			}
		}
	}

	public boolean remove(int value) {
		while (true) {
			Node previous = this.find(value);
			Node current = previous.getNext().getReference();
			if (current.getValue() != value) {
				return false;
			}
			Node next = current.getNext().getReference();
			if (current.attemptMarkNext(next, true)) {
				previous.compareAndSetNext(current, next, false, false);
				return true;
			}
		}
	}

	public boolean contains(int value) {
		Node current = m_first;
		while (current.getValue() < value) {
			current = current.getNext().getReference();
		}
		boolean[] marked = { false };
		current.getNext().get(marked);
		return current.getValue() == value && !marked[0];
	}

	public void validate() {
		java.util.Set<Integer> checker = new java.util.HashSet<>();
		int previous_value = m_first.getValue();
		Node node = m_first.getNext().getReference();
		int value = node.getValue();
		while (value < Integer.MAX_VALUE) {
			assert previous_value < value : "list is unordered: " + previous_value + " before " + value;
			assert !checker.contains(value) : "list has duplicates: " + value;
			checker.add(value);
			previous_value = value;
			node = node.getNext().getReference();
			value = node.getValue();
		}
	}
}

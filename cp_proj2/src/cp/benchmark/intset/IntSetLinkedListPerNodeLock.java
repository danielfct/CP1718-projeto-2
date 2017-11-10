package cp.benchmark.intset;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Pascal Felber
 * @author Tiago Vale
 * @since 0.1
 */
public class IntSetLinkedListPerNodeLock implements IntSet {

  public class Node {
    private final int m_value;
    private final Lock l;
    private Node m_next;

    public Node(int value, Node next) {
      m_value = value;
      m_next = next;
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

    public void lock() {
      this.l.lock();
    }

    public void unlock() {
      this.l.unlock();
    }
  }

  private final Node m_first;

  public IntSetLinkedListPerNodeLock() {
    Node min = new Node(Integer.MIN_VALUE);
    Node max = new Node(Integer.MAX_VALUE);
    min.setNext(max);
    m_first = min;
  }

  public boolean add(int value) {
    boolean result;

    Node previous = m_first;
    Node next = previous.getNext();
    int v;
    try {
      previous.lock();
      next.lock();
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
      next.unlock();
      previous.unlock();
    }
  }

  public boolean remove(int value) {
    boolean result;

    Node previous = m_first;
    Node next = previous.getNext();
    int v;
    try {
      previous.lock();
      next.lock();
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
      next.unlock();
      previous.unlock();
    }
  }

  public boolean contains(int value) {
    boolean result;

    Node previous = m_first;
    Node next = previous.getNext();
    int v;
    try {
      previous.lock();
      next.lock();
      while ((v = next.getValue()) < value) {
        previous.unlock();
        previous = next;
        next = previous.getNext();
        next.lock();
      }
      result = (v == value);

      return result;
    } finally {
      next.unlock();
      previous.unlock();
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

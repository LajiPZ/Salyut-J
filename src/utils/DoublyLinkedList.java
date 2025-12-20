package utils;

import java.util.Iterator;
import java.util.Optional;

final public class DoublyLinkedList<T> implements Iterable<DoublyLinkedList.Node<T>>{

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Node<T> getHead() {
        return head;
    }

    public Node<T> getTail() {
        return tail;
    }


    public static class Node<T> {
        private final T value;
        private Node<T> prev;
        private Node<T> next;
        private DoublyLinkedList<T> parent;

        public Node(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public Node<T> getPrev() {
            return prev;
        }

        public Node<T> getNext() {
            return next;
        }

        public void insertIntoHead(DoublyLinkedList<T> parent) {
            this.parent = parent;
            if (parent.isEmpty()) {
                parent.tail = this;
            } else {
                parent.head.prev = this;
                this.next = parent.head;
            }
            parent.head = this;
            parent.size++;
        }

        public void insertIntoTail(DoublyLinkedList<T> parent) {
            this.parent = parent;
            if (parent.isEmpty()) {
                parent.head = this;
            } else {
                parent.tail.next = this;
                this.prev = parent.tail;
            }
            parent.tail = this;
            parent.size++;
        }

        public void insertBefore(Node<T> node) {
            this.next = node;
            this.prev = node.prev;
            node.prev = this;
            Optional.ofNullable(this.prev).ifPresent(before -> before.next = this);

            this.parent = node.parent;
            this.parent.size++;

            if (parent.head == node) {
                parent.head = this;
            }
        }

        public void insertAfter(Node<T> node) {
            this.prev = node;
            this.next = node.next;
            node.next = this;
            Optional.ofNullable(this.next).ifPresent(after -> after.prev = this);

            this.parent = node.parent;
            this.parent.size++;

            if (parent.tail == node) {
                parent.tail = this;
            }
        }

        public void drop() {
            if (prev != null) {
                prev.next = next;
            } else {
                parent.head = next;
            }

            if (next != null) {
                next.prev = prev;
            } else {
                parent.tail = prev;
            }

            this.prev = null;
            this.next = null;

            parent.size--;
        }

    }


    static class ForwardIter<T> implements Iterator<Node<T>> {

        private final Node<T> start;
        private Node<T> now;

        public ForwardIter(Node<T> head) {
            this.start = new Node<>(null);
            this.now = start;
            this.now.next = head;
        }

        @Override
        public boolean hasNext() {
            return now != null && now.next != null;
        }

        @Override
        public Node<T> next() {
            now = now.next;
            return now;
        }

        @Override
        public void remove() {
            if (now == start) {
                return;
            }
            Node<T> prev = now.prev;
            Node<T> next = now.next;
            now.drop();
            if (prev == null) {
                now = start;
                start.next = next;
            } else {
                now = prev;
            }
        }
    }

    public static class BackwardIter<T> implements Iterator<Node<T>> {

        private final Node<T> start;
        private Node<T> now;

        public BackwardIter(Node<T> tail) {
            this.start = new Node<>(null);
            this.now = start;
            this.now.prev = tail;
        }

        @Override
        public boolean hasNext() {
            return now != null && now.prev != null;
        }

        @Override
        public Node<T> next() {
            now = now.prev;
            return now;
        }

        @Override
        public void remove() {
            if (now == start) {
                return;
            }
            Node<T> prev = now.prev;
            Node<T> next = now.next;
            now.drop();
            if (next == null) {
                now = start;
                start.prev = prev;
            } else {
                now = next;
            }
        }
    }

    @Override
    public Iterator<Node<T>> iterator() {
        return new ForwardIter<>(head);
    }

    public Iterator<Node<T>> iteratorAfter(Node<T> node) {
        return new ForwardIter<>(node);
    }

    public Iterator<Node<T>> backwardIterator() { return new BackwardIter<>(tail); }
}

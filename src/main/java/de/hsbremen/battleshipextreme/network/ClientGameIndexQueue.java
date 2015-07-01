package de.hsbremen.battleshipextreme.network;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by cschaf on 05.06.2015.
 * Queue, welche f�r das Verwalten der n�chsten Spielz�gen zust�ndig ist
 */
public class ClientGameIndexQueue<T> implements Iterator<T>, Serializable {
    private LinkedList<T> elements;

    public ClientGameIndexQueue() {
        elements = new LinkedList<T>();
    }

    /**
     * F�gt das angegebene Element in die Warteschlange
     */
    public void add(T element) {
        elements.add(element);
    }

    /**
     * Gibt des n�chste Element der Warteschlange zur�ck aber entfernt es nicht
     */
    public T peek() {
        return elements.getFirst();
    }

    /**
     * Entfernt alle Elemente aus der Warteschlange
     */
    public void clear() {
        elements.clear();
    }

    /**
     * Gibt die Anzahl der Elemente in der Warteschlange zur�ck
     */
    public int size() {
        return elements.size();
    }

    /**
     * Gibt true zur�ck wenn die Warteschlange leer ist
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Gibt einen Iterator f�r die Warteschlange zur�ck
     */
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    /**
     * Gibt true zur�ck wenn es ein  n�chstes Element gibt
     */
    public boolean hasNext() {
        return elements.iterator().hasNext();
    }

    /**
     * Entfernt das n�chste Element aus der Warteschlange
     */

    public T next() {
        return elements.removeFirst();
    }

    /**
     * Entfernt das erste Element der Warteschlange
     */
    public void remove() {
        elements.remove();
    }
}


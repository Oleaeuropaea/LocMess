package pt.ulisboa.tecnico.cmu.locmess.utils;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;



public final class LocMessLinkedHashSet<E> implements Set<E>, Serializable {
    private final ArrayList<E> list = new ArrayList<>( );
    private final HashSet<E>   set  = new HashSet<>( );

    public synchronized E get(int index) {
        return list.get(index);
    }

    @Override
    public synchronized int size() {
        return set.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return set.contains(o);
    }

    @NonNull
    @Override
    public synchronized Iterator<E> iterator() {
        return list.iterator();
    }

    @NonNull
    @Override
    public synchronized Object[] toArray() {
        return list.toArray();
    }

    @NonNull
    @Override
    public synchronized  <T> T[] toArray(@NonNull T[] a) {
        return list.toArray(a);
    }

    @Override
    public synchronized boolean add(E e) {
        if ( set.add(e) ) {
            return list.add(e);
        }
        return false;
    }

    @Override
    public synchronized boolean remove(Object o) {
        if ( set.remove(o) ) {
            return list.remove(o);
        }
        return false;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(@NonNull Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c)
            if (add(e))
                modified = true;
        return modified;
    }

    @Override
    public synchronized boolean retainAll(@NonNull Collection<?> c) {
        if ( set.retainAll(c) ) {
            return list.retainAll(c);
        }
        return false;
    }

    @Override
    public synchronized boolean removeAll(@NonNull Collection<?> c) {
        if ( set.removeAll(c) ) {
            return list.removeAll(c);
        }
        return true;
    }

    @Override
    public void clear() {
        set.clear();
        list.clear();
    }
}

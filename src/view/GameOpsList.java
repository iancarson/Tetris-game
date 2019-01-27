package view;


import controller.Movable;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class GameOpsList extends LinkedList {

    private ReentrantLock lock;

    public GameOpsList() {
        this.lock =   new ReentrantLock();
    }

    public void enqueue(Movable mov, CollisionOp.Operation operation) {

        try {
            lock.lock();
            addLast(new CollisionOp(mov, operation));
        } finally {
            lock.unlock();
        }
    }
//controls the threads in  play.

    public CollisionOp dequeue() {
        try {
            lock.lock();
            return (CollisionOp) super.removeFirst();
        } finally {
            lock.unlock();
        }

    }
}


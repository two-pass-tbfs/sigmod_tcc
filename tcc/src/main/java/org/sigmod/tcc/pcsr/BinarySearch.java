package org.sigmod.tcc.pcsr;

/**
 * Searches the element from the edge list using a binary search algorithm.
 */
public class BinarySearch {
    public static int compute(EdgeList list, PEdge elem, int start, int end) {
        while (start + 1 < end) {
            int mid = (start + end) / 2;

            PEdge item = list.getItem(mid);
            int change = 1;
            int check = mid;

            boolean flag = true;
            while (item.isNull() && flag) {
                flag = false;
                check = mid + change;
                if (check < end) {
                    flag = true;
                    if (check <= end) {
                        item = list.getItem(check);
                        if (!item.isNull() || check == end) break;
                    }
                }
                check = mid - change;
                if (check >= start) {
                    flag = true;
                    item = list.getItem(check);
                }
                change++;

            }

            if (item.isNull() || start == check || end == check) {
                if (!item.isNull() && start == check && elem.getDest() <= item.getDest()) return check;
                return mid;
            }

            // if elem is found, return
            if (elem.getDest() == item.getDest()) return check;
            else if (elem.getDest() < item.getDest()) end = check;
            else start = check;
        }
        if (end < start) start = end;

        if (elem.getDest() <= list.getItem(start).getDest() && !list.getItem(start).isNull()) return start;

        return end;
    }
}

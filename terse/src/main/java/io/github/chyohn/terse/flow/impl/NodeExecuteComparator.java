package io.github.chyohn.terse.flow.impl;

import io.github.chyohn.terse.flow.ITask;
import java.util.Comparator;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
class NodeExecuteComparator implements Comparator<Node> {
    private static final NodeExecuteComparator INSTANCE = new NodeExecuteComparator();
    public static NodeExecuteComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(Node n1, Node n2) {

        // 先对把task类型的放前面
        // 如果只有第一个是task，需排前面，返回小于0的数
        boolean n1IsTask = n1 instanceof ITask;
        boolean n2IsTask = n2 instanceof ITask;
        if (n1IsTask && !n2IsTask) {
            return -1;
        }
        // 如果只有第二个是task，则把第一个要排后面，返回大于0的数
        if (!n1IsTask && n2IsTask) {
            return 1;
        }

        // 先根据直接子节点最小级别升序排序
        int comp = Integer.compare(n1.getChildrenMinLevel(), n2.getChildrenMinLevel());
        if (comp != 0) {
            return comp;
        }

        // 同最小子级别中，按照自定义升序排序
        return Integer.compare(n1.getTask().sorted(), n2.getTask().sorted());
    }
}

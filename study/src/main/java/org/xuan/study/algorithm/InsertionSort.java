package org.xuan.study.algorithm;

/**
 * 插入法排序
 * 
 * @author liu.xuan
 *
 */
public class InsertionSort {

	public void sort(int[] array) {
		for (int j = 1; j < array.length; j++) {
			int key = array[j];
			int i = j - 1;
			while (i >= 0 && array[i] > key) {
				array[i + 1] = array[i];
				i--;
			}
			array[i + 1] = key;
		}
	}

	public static void main(String[] args) {
		int[] sortArray = new int[] { 5, 6, 1, 8, 3, 7, 2, 9 };
		InsertionSort sort = new InsertionSort();
		sort.sort(sortArray);
		for (int i = 0; i < sortArray.length; i++) {
			int item = sortArray[i];
			System.out.print(item + ",");
		}

	}

}

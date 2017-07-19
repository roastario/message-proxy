package com.stefano.weaveworks;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * Author stefanofranz
 */
public class MessageTest {

    @Test(timeout = 2000L)
    public void comparatorShouldSortOldestMessageFirst() throws Exception {

        Message message1 = new Message(null, null, 1, null, 1001);
        Message message2 = new Message(null, null, 1, null, 1002);
        Message message3 = new Message(null, null, 1, null, 1003);
        Message message4 = new Message(null, null, 1, null, 1004);
        Message message5 = new Message(null, null, 1, null, 1005);
        Message message6 = new Message(null, null, 1, null, 1006);


        ArrayList<Message> unsorted = new ArrayList<>(Arrays.asList(message4, message1, message2, message6, message3, message5));
        unsorted.sort(Message.oldestMessageFirst());
        Assert.assertThat(unsorted, contains(message1, message2, message3, message4, message5, message6));

    }
}
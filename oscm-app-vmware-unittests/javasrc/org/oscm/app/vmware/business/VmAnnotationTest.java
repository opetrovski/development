/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2016                                           
 *                                                                                                                                 
 *  Creation Date: 17.06.2016                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.vmware.business;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * @author kulle
 *
 */
public class VmAnnotationTest {

    private static final String COMMENT_OPENING_TAG = "CT-MG {\n";
    private static final String COMMENT_CLOSING_TAG = "\n}";

    private VM vm;
    private String comment;

    @Before
    public void before() throws Exception {
        vm = new VM();
        comment = "Organization: Supplier\nSubscription-Id: CentOS 5.2\nCreator: supplier";
    }

    @Test
    public void updateComment_nullAnnotation() {
        // given
        String annotation = null;

        // when
        String result = vm.updateComment(comment, annotation);

        // then
        assertEquals(COMMENT_OPENING_TAG + comment + COMMENT_CLOSING_TAG,
                result);
    }

    @Test
    public void updateComment_emptyAnnotation() {
        // given
        String annotation = "";

        // when
        String result = vm.updateComment(comment, annotation);

        // then
        assertEquals(COMMENT_OPENING_TAG + comment + COMMENT_CLOSING_TAG,
                result);
    }

    @Test
    public void updateComment_existingAnnotation_noComment() {
        // given
        String annotation = "existing annotation";

        // when
        String result = vm.updateComment(comment, annotation);

        // then
        assertEquals(annotation + "\n" + COMMENT_OPENING_TAG + comment
                + COMMENT_CLOSING_TAG, result);
    }

    @Test
    public void updateComment() {
        // given
        String annotation = "existing annotation with comment\n"
                + COMMENT_OPENING_TAG + comment + COMMENT_CLOSING_TAG;

        String newComment = comment.replace("supplier", "supplier2");

        // when
        String result = vm.updateComment(newComment, annotation);

        // then
        assertEquals("existing annotation with comment\n" + COMMENT_OPENING_TAG
                + newComment + COMMENT_CLOSING_TAG, result);
    }

}

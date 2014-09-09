package gsn.aws;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.CreateStreamRequest;
import com.amazonaws.services.kinesis.model.DeleteStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.ListStreamsRequest;
import com.amazonaws.services.kinesis.model.ListStreamsResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;


public class KinesisConnect {

	 static AmazonKinesisClient kinesisClient;
	    private static final Log LOG = LogFactory.getLog(AmazonKinesisSample.class);

	    private static void init() throws Exception {

	        /*
	         * The ProfileCredentialsProvider will return your [default]
	         * credential profile by reading from the credentials file located at
	         * (~/.aws/credentials).
	         */
	        AWSCredentials credentials = null;
	        try {
	            credentials = new ProfileCredentialsProvider().getCredentials();
	        } catch (Exception e) {
	            throw new AmazonClientException(
	                    "Cannot load the credentials from the credential profiles file. " +
	                    "Please make sure that your credentials file is at the correct " +
	                    "location (~/.aws/credentials), and is in valid format.",
	                    e);
	        }

	        kinesisClient = new AmazonKinesisClient(credentials);
	    }

	/**
	 * @param args
	 * @throws Exception 
	 */
	    
	    public static void pushToStream(String se) throws Exception{
	    	 	init();

			   /*
			    * 
			    1. Create automation of Stream Generation on the basis of the source that sends the data
			    2. Create shards in the stream based on the data
			    3. Put data in each stream from the data that has been sent through Arguments
			    */
			   
		        final String myStreamName = "myFirstStream";
		        final Integer myStreamSize = 1;

		        // Create a stream. The number of shards determines the provisioned throughput.

		        CreateStreamRequest createStreamRequest = new CreateStreamRequest();
		        createStreamRequest.setStreamName(myStreamName);
		        createStreamRequest.setShardCount(myStreamSize);

		        kinesisClient.createStream(createStreamRequest);
		        // The stream is now being created.
		        LOG.info("Creating Stream : " + myStreamName);
		        waitForStreamToBecomeAvailable(myStreamName);

		        // list all of my streams
		        ListStreamsRequest listStreamsRequest = new ListStreamsRequest();
		        listStreamsRequest.setLimit(10);
		        ListStreamsResult listStreamsResult = kinesisClient.listStreams(listStreamsRequest);
		        
		        List<String> streamNames = listStreamsResult.getStreamNames();
		        while (listStreamsResult.isHasMoreStreams()) {
		            if (streamNames.size() > 0) {
		                listStreamsRequest.setExclusiveStartStreamName(streamNames.get(streamNames.size() - 1));
		            }

		            listStreamsResult = kinesisClient.listStreams(listStreamsRequest);
		            streamNames.addAll(listStreamsResult.getStreamNames());

		        }
		        LOG.info("Printing my list of streams : ");

		        // print all of my streams.
		        if (!streamNames.isEmpty()) {
		            System.out.println("List of my streams: ");
		        }
		        for (int i = 0; i < streamNames.size(); i++) {
		            System.out.println(streamNames.get(i));
		        }

		        LOG.info("Putting records in stream : " + myStreamName);
		        // Write 10 records to the stream
		        System.out.println("Stream is:"+se);
		       /* for (int j = 0; j < 10; j++) {
		            PutRecordRequest putRecordRequest = new PutRecordRequest();
		            putRecordRequest.setStreamName(myStreamName);
		            putRecordRequest.setData(ByteBuffer.wrap(String.format("testData-%d", j).getBytes()));
		            putRecordRequest.setPartitionKey(String.format("partitionKey-%d", j));
		            PutRecordResult putRecordResult = kinesisClient.putRecord(putRecordRequest);
		            System.out.println("Successfully putrecord, partition key : " + putRecordRequest.getPartitionKey()
		                    + ", ShardID : " + putRecordResult.getShardId());
		        }*/	

	    }
	public static void main(String[] args) throws Exception {
		  
	        // Delete the stream.
	       // LOG.info("Deleting stream : " + myStreamName);
	        //DeleteStreamRequest deleteStreamRequest = new DeleteStreamRequest();
	        //deleteStreamRequest.setStreamName(myStreamName);

	        //kinesisClient.deleteStream(deleteStreamRequest);
	        // The stream is now being deleted.
	        //LOG.info("Stream is now being deleted : " + myStreamName);
	    }

	    private static void waitForStreamToBecomeAvailable(String myStreamName) {

	        System.out.println("Waiting for " + myStreamName + " to become ACTIVE...");

	        long startTime = System.currentTimeMillis();
	        long endTime = startTime + (10 * 60 * 1000);
	        while (System.currentTimeMillis() < endTime) {
	            try {
	                Thread.sleep(1000 * 20);
	            } catch (InterruptedException e) {
	                // Ignore interruption (doesn't impact stream creation)
	            }
	            try {
	                DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest();
	                describeStreamRequest.setStreamName(myStreamName);
	                // ask for no more than 10 shards at a time -- this is an optional parameter
	                describeStreamRequest.setLimit(10);
	                DescribeStreamResult describeStreamResponse = kinesisClient.describeStream(describeStreamRequest);

	                String streamStatus = describeStreamResponse.getStreamDescription().getStreamStatus();
	                System.out.println("  - current state: " + streamStatus);
	                if (streamStatus.equals("ACTIVE")) {
	                    return;
	                }
	            } catch (AmazonServiceException ase) {
	                if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == false) {
	                    throw ase;
	                }
	                throw new RuntimeException("Stream " + myStreamName + " never went active");
	            }
	        }
	    }

}
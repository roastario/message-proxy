package com.stefano.sewworks.stats;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Author stefanofranz
 */
public class StatsJSONifier {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final PrintStream output;

    public StatsJSONifier(PrintStream output) {
        this.output = output;
    }

    public void jsonify(long totalMessages,
                        long reqTotal,
                        long ackTotal,
                        long nakTotal,
                        double requestRateOneSecondWindow,
                        double requestRateTenSecondWindow,
                        double responseRateOneSecondWindow,
                        double responseRateTenSecondWindow) {
        gson.toJson(new Stats(
                        totalMessages,
                        reqTotal,
                        ackTotal,
                        nakTotal,
                        requestRateOneSecondWindow,
                        requestRateTenSecondWindow,
                        responseRateOneSecondWindow,
                        responseRateTenSecondWindow
                ),
                output
        );
        output.flush();

    }

    public void jsonify(StatsCollector statsCollector, long startTime) {
        this.jsonify(statsCollector.getTotalMessages(),
                statsCollector.getRequestTotal(),
                statsCollector.getAckTotal(),
                statsCollector.getNakTotal(),
                statsCollector.getRequestRateWithinWindow(1, TimeUnit.SECONDS, startTime),
                statsCollector.getRequestRateWithinWindow(10, TimeUnit.SECONDS, startTime),
                statsCollector.getResponseRateWithinWindow(1, TimeUnit.SECONDS, startTime),
                statsCollector.getResponseRateWithinWindow(10, TimeUnit.SECONDS, startTime)
        );
    }

    private static final class Stats {

        @SerializedName("msg_total")
        private final long totalMessages;
        @SerializedName("msg_req")
        private final long reqTotal;
        @SerializedName("msg_ack")
        private final long ackTotal;
        @SerializedName("msg_nak")
        private final long nakTotal;

        @SerializedName("request_rate_1s")
        private final double requestRateOneSecondWindow;
        @SerializedName("request_rate_10s")
        private final double requestRateTenSecondWindow;

        @SerializedName("response_rate_1s")
        private final double responseRateOneSecondWindow;
        @SerializedName("response_rate_10s")
        private final double responseRateTenSecondWindow;

        private Stats(long totalMessages,
                      long reqTotal,
                      long ackTotal,
                      long nakTotal,
                      double requestRateOneSecondWindow,
                      double requestRateTenSecondWindow,
                      double responseRateOneSecondWindow,
                      double responseRateTenSecondWindow) {
            this.totalMessages = totalMessages;
            this.reqTotal = reqTotal;
            this.ackTotal = ackTotal;
            this.nakTotal = nakTotal;
            this.requestRateOneSecondWindow = requestRateOneSecondWindow;
            this.requestRateTenSecondWindow = requestRateTenSecondWindow;
            this.responseRateOneSecondWindow = responseRateOneSecondWindow;
            this.responseRateTenSecondWindow = responseRateTenSecondWindow;
        }


        public long getTotalMessages() {
            return totalMessages;
        }

        public long getReqTotal() {
            return reqTotal;
        }

        public long getAckTotal() {
            return ackTotal;
        }

        public long getNakTotal() {
            return nakTotal;
        }

        public double getRequestRateOneSecondWindow() {
            return requestRateOneSecondWindow;
        }

        public double getRequestRateTenSecondWindow() {
            return requestRateTenSecondWindow;
        }

        public double getResponseRateOneSecondWindow() {
            return responseRateOneSecondWindow;
        }

        public double getResponseRateTenSecondWindow() {
            return responseRateTenSecondWindow;
        }
    }


}

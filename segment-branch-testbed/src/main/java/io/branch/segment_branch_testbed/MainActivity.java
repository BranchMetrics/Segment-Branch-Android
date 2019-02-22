package io.branch.segment_branch_testbed;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;

import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.android.integrations.branch.BranchIntegration;

import java.util.UUID;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Analytics analytics = new Analytics.Builder(MainActivity.this, getString(R.string.segment_write_key))
                .use(BranchIntegration.FACTORY)
                .build();


        findViewById(R.id.cmdIdentifyUser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                analytics.identify(UUID.randomUUID().toString());
            }
        });

        findViewById(R.id.cmdTrackEvent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String eventName = (String) ((Spinner) findViewById(R.id.event_name_spinner)).getSelectedItem();
                Properties properties = new Properties()
                        .putCategory("my category")
                        .putCoupon("my coupon")
                        .putCurrency("USD")
                        .putDiscount(30.00)
                        .putName("my property name")
                        .putOrderId("my order id")
                        .putPath("/my_path")
                        .putPrice(20.50)
                        .putProductId("my_prod_id")
                        .putReferrer("my referrer string")
                        .putRevenue(12.9)
                        .putShipping(2.05)
                        .putSku("my sku")
                        .putTax(3.0)
                        .putTitle("my title")
                        .putUrl("https://my_test_url")
                        .putTotal(22.05);

                Properties.Product product1 = new Properties.Product("prod1_id", "prod1_sku", 12.00);
                Properties.Product product2 = new Properties.Product("prod2_id", "prod2_sku", 13.00);
                properties.putProducts(product1, product2);
                analytics.track(eventName, properties);
            }
        });


        findViewById(R.id.cmdTrackView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                analytics.screen(MainActivity.class.getName());
            }
        });

        findViewById(R.id.cmdReset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                analytics.reset();
            }
        });

        findViewById(R.id.cmdAlias).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                analytics.alias(UUID.randomUUID().toString());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}

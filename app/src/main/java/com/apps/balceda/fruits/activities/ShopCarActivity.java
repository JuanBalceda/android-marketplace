package com.apps.balceda.fruits.activities;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.balceda.fruits.activities.home.HomeActivity;
import com.apps.balceda.fruits.R;
import com.apps.balceda.fruits.adapters.ShopCarAdapter;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import java.util.Arrays;

public class ShopCarActivity extends AppCompatActivity {

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 0;
    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;
    ShopCarAdapter adapter;
    Button checkout;

    //Shopping Car
    static TextView subTotal, igv, total;

    //Payment
    private PaymentsClient mPaymentsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_car);

        //Payment
        mPaymentsClient =
                Wallet.getPaymentsClient(
                        this,
                        new Wallet.WalletOptions.Builder()
                                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                                .build());

        setTitle("Carrito");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // List
        recycler_menu = findViewById(R.id.recycler_shop_car);
        recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_menu.setLayoutManager(layoutManager);
        adapter = new ShopCarAdapter(getApplicationContext(), HomeActivity.finalOrder);
        recycler_menu.setAdapter(adapter);

        subTotal = findViewById(R.id.subtotal_amount);
        igv = findViewById(R.id.igv_amount);
        total = findViewById(R.id.total);

        calculate();

        checkout = findViewById(R.id.buy);
        checkout.setOnClickListener((v) -> {
            Toast.makeText(getApplicationContext(), "A pagar! $_$!!!", Toast.LENGTH_LONG).show();
            isReadyToPay();

            PaymentDataRequest request = createPaymentDataRequest();
            if (request != null) {
                AutoResolveHelper.resolveTask(
                        mPaymentsClient.loadPaymentData(request),
                        this,
                        // LOAD_PAYMENT_DATA_REQUEST_CODE is a constant value
                        // you define.
                        LOAD_PAYMENT_DATA_REQUEST_CODE);
            }
        });
    }

    //Calculate Final Amount
    public static void calculate() {
        double finalSubtotalAmount = 0.0;
        double finalIgvAmount = 0.0;
        double finalTotalAmount = 0.0;

        if (HomeActivity.finalOrder.size() > 0) {
            for (int i = 0; i < HomeActivity.finalOrder.size(); i++) {
                finalSubtotalAmount += HomeActivity.finalOrder.get(i).getSubTotal();
                finalIgvAmount += HomeActivity.finalOrder.get(i).getIgvAmount();
                finalTotalAmount += HomeActivity.finalOrder.get(i).getTotal();
            }
        } else {
            finalSubtotalAmount = 0;
            finalIgvAmount = 0;
            finalTotalAmount = 0;
        }
        subTotal.setText(String.format("S/ %1$,.2f", finalSubtotalAmount));
        igv.setText(String.format("S/  %1$,.2f", finalIgvAmount));
        total.setText(String.format("S/  %1$,.2f", finalTotalAmount));
    }

    //Go Back to home Activity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void isReadyToPay() {
        IsReadyToPayRequest request =
                IsReadyToPayRequest.newBuilder()
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .build();
        Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
        task.addOnCompleteListener((booleanTask) -> {
            try {
                boolean result = booleanTask.getResult(ApiException.class);
                if (result == true) {
                    // Show Google as payment option.
                } else {
                    // Hide Google as payment option.
                }
            } catch (ApiException exception) {
                // Handle exception
            }

        });
    }

    private PaymentDataRequest createPaymentDataRequest() {
        PaymentDataRequest.Builder request =
                PaymentDataRequest.newBuilder()
                        .setTransactionInfo(
                                TransactionInfo.newBuilder()
                                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                        .setTotalPrice("1.00")
                                        .setCurrencyCode("USD")
                                        .build())
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .setCardRequirements(
                                CardRequirements.newBuilder()
                                        .addAllowedCardNetworks(
                                                Arrays.asList(
                                                        WalletConstants.CARD_NETWORK_AMEX,
                                                        WalletConstants.CARD_NETWORK_DISCOVER,
                                                        WalletConstants.CARD_NETWORK_VISA,
                                                        WalletConstants.CARD_NETWORK_MASTERCARD))
                                        .build());

        PaymentMethodTokenizationParameters params =
                PaymentMethodTokenizationParameters.newBuilder()
                        .setPaymentMethodTokenizationType(
                                WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                        .addParameter("gateway", "stripe")
                        .addParameter("stripe:publishableKey", "pk_test_N9MHqeDKqEIdUdyKGAwgoFMW")
                        .addParameter("stripe:version", "5.1.0")
                        .build();

        request.setPaymentMethodTokenizationParameters(params);
        return request.build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        String token = paymentData.getPaymentMethodToken().getToken();
                        CardInfo info = paymentData.getCardInfo();

                        Toast.makeText(getApplicationContext(),"Gracias por tu compra, "+info.getCardDescription(), Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getApplicationContext(),"No se realizó la compra", Toast.LENGTH_SHORT).show();
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Toast.makeText(getApplicationContext(),"Ocurrió un error", Toast.LENGTH_SHORT).show();
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        // Log the status for debugging.
                        // Generally, there is no need to show an error to
                        // the user as the Google Pay API will do that.
                        break;
                    default:
                        // Do nothing.
                }
                break;
            default:
                // Do nothing.
        }

    }
}

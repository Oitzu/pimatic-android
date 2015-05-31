package org.pimatic.accounts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONObject;
import org.pimatic.connection.RestClient;
import org.pimatic.model.ConnectionOptions;

public class PimaticAccountAuthenticator extends AbstractAccountAuthenticator {
    private final Context mContext;
    private String TAG = this.getClass().getSimpleName();

    public PimaticAccountAuthenticator(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "addAccount");

        final Intent intent = new Intent(mContext, PimaticAccountAuthenticatorActivity.class);
        intent.putExtra(PimaticAccountAuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(PimaticAccountAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(PimaticAccountAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {

        Log.d(TAG, "getAuthToken: getAuthToken");

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);

        String authToken = am.peekAuthToken(account, authTokenType);

        Log.d(TAG, "getAuthToken: peekAuthToken returned - " + authToken);
        String authUrl = am.getUserData(account, AccountGeneral.ACCOUNT_USER_DATA_URL);
        ConnectionOptions conOpts = ConnectionOptions.fromAuthToken(authUrl);

        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken)) {
            final String password = am.getPassword(account);
            if (password != null) {
                try {

                    RestClient client = new RestClient(mContext, conOpts);
                    Log.v(TAG, authUrl + " => " + conOpts.toString());
                    final RequestFuture<JSONObject> future = RequestFuture.newFuture();
                    client.login(conOpts.username, conOpts.password, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            future.onResponse(jsonObject);
                        }
                    }, new RestClient.ErrorToater(mContext) {
                        @Override
                        public void onNetworkError(VolleyError error) {
                            super.onNetworkError(error);
                            future.onErrorResponse(error);
                        }

                        @Override
                        public void onJsonResult(VolleyError error, JSONObject errorResult) {
                            super.onJsonResult(error, errorResult);
                            future.onResponse(errorResult);
                        }
                    });

                    Log.d(TAG, "re-authenticating with the existing password");

                    JSONObject result = future.get();
                    if (result.getBoolean("success")) {
                        authToken = authUrl;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(mContext, PimaticAccountAuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(PimaticAccountAuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(PimaticAccountAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(PimaticAccountAuthenticatorActivity.ARG_ACCOUNT_NAME, account.name);
        conOpts.putInIntent(intent);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }


    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return "Full Access";
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }
}
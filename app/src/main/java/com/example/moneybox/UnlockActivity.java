package com.example.moneybox;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import static com.example.moneybox.MainActivity.fileIsExists;
import static com.example.moneybox.util.DateUtil.getTime;
import static com.example.moneybox.util.DateUtil.getTodayDate;

public class UnlockActivity extends AppCompatActivity {

    //SocketClient socket = SocketClient.getInstance();
    private mDatabaseHelper dbHelper = new mDatabaseHelper(this, "Deposit.db", null, 2);
    private String password;
    private static final String TAG = "UnlockActivity";
    boolean isSettingPassword = false;
    boolean hasWithdrawMoney = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("开锁");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences pref = getSharedPreferences("data",MODE_PRIVATE);
        password = pref.getString("PASSWORD", "123456");


        Button button = findViewById(R.id.btn_unlock);
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                /*if (socket != null)//TODO：这个是长按的后门
                    if (socket.getIsConnected())
                        socket.sendMessage("UNLOCK");*/
                return true;
            }
        });

        Log.d(TAG, "onCreate: execute");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_unlock_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Log.d(TAG, "onOptionsItemSelected: yesssssssssssssss");
                Intent intent = new Intent();
                intent.putExtra("hasWithdrawMoney", hasWithdrawMoney);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            }
            case R.id.item_unlock_change_password: {
                EditText editPassword = findViewById(R.id.et_password);
                EditText editNewPassword = findViewById(R.id.et_new_password);
                Button btnSetNewPassword = findViewById(R.id.btn_unlock);
                editPassword.setText("");
                editPassword.setHint("请输入旧密码");
                editNewPassword.setVisibility(View.VISIBLE);
                btnSetNewPassword.setText("确认新密码");
                isSettingPassword = true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        /*if (socket.getIsConnected()) {
            socket.disconnectSocketServer();
        }*/
        Log.d(TAG, "onBackPressed: yesssssssssssss");
        Intent intent = new Intent();
        intent.putExtra("hasWithdrawMoney", hasWithdrawMoney);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    public void unlock(View view) {
        if (isSettingPassword) {                                        //设置密码
            EditText editPassword = findViewById(R.id.et_password);
            EditText editNewPassword = findViewById(R.id.et_new_password);
            Button btnSetNewPassword = findViewById(R.id.btn_unlock);
            String password = editPassword.getText().toString();
            if (password.equals(this.password)) {
                String NewPassword = editNewPassword.getText().toString();
                SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
                editor.putString("PASSWORD", NewPassword);
                editor.apply();

                editPassword.setText("");
                editPassword.setHint("输入密码");
                editNewPassword.setVisibility(View.INVISIBLE);
                btnSetNewPassword.setText("开锁");

                this.password = NewPassword;
                isSettingPassword = false;
            } else {
                Toast.makeText(UnlockActivity.this, "密码输入错误", Toast.LENGTH_SHORT).show();
            }
        } else {                                                        //真的在开锁
            if (isPasswordCorrect()) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(UnlockActivity.this);
                final EditText editText = new EditText(UnlockActivity.this);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
                dialog.setView(editText);
                dialog.setTitle("输入取钱金额");

                SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
                final int totalVal = pref.getInt("TotalVal", 0);
                dialog.setCancelable(true);
                dialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if ( editText.getText().toString().equals("")) {
                            Toast.makeText(UnlockActivity.this, "尚未输入任何金额", Toast.LENGTH_SHORT).show();
                        } else if (Integer.parseInt(editText.getText().toString()) > totalVal) {
                            Toast.makeText(UnlockActivity.this, "好像没有那么多钱", Toast.LENGTH_SHORT).show();
                        } else {
                            int OutputVal = Integer.parseInt(editText.getText().toString());
                            OutputVal = -1 * OutputVal;
                            SaveMoney saveMoney = new SaveMoney();
                            saveMoney.setUpdateDate(getTodayDate());
                            saveMoney.setUpdateTime(getTime());
                            saveMoney.setValue(OutputVal);
                            Toast.makeText(UnlockActivity.this, "你取出了" + saveMoney.getValue(), Toast.LENGTH_SHORT).show();
                            EditText editPassword = findViewById(R.id.et_password);
                            editPassword.setText("");
                            //清理数据库，让用户输入取出的钱的数目
                            if (fileIsExists(UnlockActivity.this.getApplication().getFilesDir().getParentFile().getPath()+"/databases/Deposit.db")) {
                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                // db.delete("Deposit", null, null);

                                ContentValues values = new ContentValues();
                                values.put("updateDate", saveMoney.getUpdateDate());
                                values.put("updateTime", saveMoney.getUpdateTime());
                                values.put("value", saveMoney.getValue());
                                db.insert("Deposit", null, values);
                                values.clear();
                                Log.d(TAG, "unlock: Already save new data to Deposit Table");

                                //更新数据TotalVal到SharedPreference
                                SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
                                SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
                                int lastTotalVal = pref.getInt("TotalVal", 0);
                                int TotalVal = saveMoney.getValue() + lastTotalVal;
                                editor.putInt("TotalVal", TotalVal);   //储存总金额，方便下一次启动时显示
                                editor.apply();
                                Log.d(TAG, "run: TotalVal " + TotalVal +" is saved at SharedPreferences data.xml");

                                /*//把存钱数据即刻更新到存钱宝中
                                //socket.sendMessage(TmpGoal + "GOAL" + TmpVal + "VAL");*/
                                //TODO


                                //然后再更新DailyDeposit
                                //存入数据库的DailyDeposit表中
                                Cursor cursor = db.query("DailyDeposit", null, null, null, null, null, null);
                                if (cursor.moveToLast()) {
                                    String lastDate = cursor.getString(cursor.getColumnIndex("updateDate"));
                                    Log.d(TAG, "storeESP8266Data:  " + saveMoney.getUpdateDate() + "  " + lastDate);
                                    if (saveMoney.getUpdateDate().equals(lastDate)) {
                                        //
                                        int lastValue = cursor.getInt(cursor.getColumnIndex("value"));
                                        values.put("value", (saveMoney.getValue() + lastValue));
                                        db.update("DailyDeposit", values, "updateDate=?", new String[]{lastDate} );
                                        values.clear();
                                        Log.d(TAG, "storeESP8266Data: Already save new data to DailyDeposit Table");
                                    } else {
                                        values.put("updateDate", saveMoney.getUpdateDate());
                                        values.put("value", saveMoney.getValue());
                                        db.insert("DailyDeposit", null, values);
                                        values.clear();
                                        Log.d(TAG, "storeESP8266Data: Already save new data to DailyDeposit Table");
                                    }
                                } else {
                                    values.put("updateDate", saveMoney.getUpdateDate());
                                    values.put("value", saveMoney.getValue());
                                    db.insert("DailyDeposit", null, values);
                                    values.clear();
                                    Log.d(TAG, "storeESP8266Data: Already save new data to DailyDeposit Table");
                                }
                                cursor.close();

                                //还要通知MainActivity更新saveMoneyList
                                hasWithdrawMoney = true;
                                editor.putBoolean("hasWithdrawMoney", true);
                                editor.apply();
                            }
                        }

                    }
                });
                dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(UnlockActivity.this, "取消了", Toast.LENGTH_SHORT).show();
                        EditText editPassword = findViewById(R.id.et_password);
                        editPassword.setText("");
                    }
                });
                dialog.show();
            }

        }
    }

    public boolean isPasswordCorrect() {
        EditText editText = findViewById(R.id.et_password);
        String password = editText.getText().toString();
        if (password.equals(this.password)) {
            Log.d(TAG, "isPasswordCorrect: password correct!");
            return true;
            /*if (socket != null)
                if (socket.getIsConnected())
                    socket.sendMessage("UNLOCK");*/
        } else {
            editText.setText("");
            Toast.makeText(UnlockActivity.this, "密码输入错误", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "isPasswordCorrect: password is not correct!");
            return false;
        }
        

    }
}

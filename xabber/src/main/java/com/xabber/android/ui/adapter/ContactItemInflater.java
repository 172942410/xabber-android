package com.xabber.android.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.android.data.extension.capability.ClientSoftware;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.activity.ContactEditor;
import com.xabber.android.ui.activity.ContactViewer;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.Emoticons;
import com.xabber.android.utils.StringUtils;

import java.io.File;
import java.util.Date;

public class ContactItemInflater {

    final Context context;

    public ContactItemInflater(Context context) {
        this.context = context;
    }

    public View setUpContactView(View convertView, ViewGroup parent, final AbstractContact contact) {
        final View view;
        final ContactListItemViewHolder viewHolder;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
            viewHolder = new ContactListItemViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ContactListItemViewHolder) view.getTag();
        }

        if (AccountManager.getInstance().getAccount(contact.getAccount()).getState() == ConnectionState.connected) {
            viewHolder.offlineShadow.setVisibility(View.GONE);
        } else {
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        }

        viewHolder.color.setImageDrawable(new ColorDrawable(ColorManager.getInstance().getAccountPainter().getAccountMainColor(contact.getAccount())));
        viewHolder.color.setVisibility(View.VISIBLE);

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatar.setVisibility(View.VISIBLE);
            viewHolder.avatar.setImageDrawable(contact.getAvatarForContactList());
        } else {
            viewHolder.avatar.setVisibility(View.GONE);
        }

        viewHolder.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAvatarClick(contact);
            }
        });

        viewHolder.name.setText(contact.getName());

        MessageManager messageManager = MessageManager.getInstance();
        if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            viewHolder.name.setTextColor(ColorManager.getInstance().getColorMucPrivateChatText());
        } else if (messageManager.hasActiveChat(contact.getAccount(), contact.getUser())) {
            viewHolder.name.setTextColor(ColorManager.getInstance().getActiveChatTextColor());
        } else {
            viewHolder.name.setTextColor(ColorManager.getInstance().getColorMain());
        }

        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            viewHolder.mucIndicator.setVisibility(View.VISIBLE);
            viewHolder.mucIndicator.setImageResource(R.drawable.ic_muc_indicator_black_16dp);
        } else if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            viewHolder.mucIndicator.setVisibility(View.VISIBLE);
            viewHolder.mucIndicator.setImageResource(R.drawable.ic_muc_private_chat_indicator_black_16dp);
        } else {
            viewHolder.mucIndicator.setVisibility(View.GONE);
        }

        String statusText;

        viewHolder.outgoingMessageIndicator.setVisibility(View.GONE);



        viewHolder.smallRightText.setVisibility(View.GONE);
        viewHolder.smallRightIcon.setVisibility(View.GONE);

        ClientSoftware clientSoftware = contact.getClientSoftware();
        if (clientSoftware == ClientSoftware.unknown) {
            viewHolder.largeClientIcon.setVisibility(View.GONE);
        } else {
            viewHolder.largeClientIcon.setVisibility(View.VISIBLE);
            viewHolder.largeClientIcon.setImageLevel(clientSoftware.ordinal());
        }

        MessageItem lastMessage = messageManager.getOrCreateChat(contact.getAccount(), contact.getUser()).getLastMessage();

        if (lastMessage == null) {
            statusText = contact.getStatusText().trim();
        } else {
            if (lastMessage.getFilePath() != null) {
                statusText = new File(lastMessage.getFilePath()).getName();
            } else {
                statusText = lastMessage.getText().trim();
            }

            viewHolder.smallRightText.setText(StringUtils.getSmartTimeText(context, new Date(lastMessage.getTimestamp())));
            viewHolder.smallRightText.setVisibility(View.VISIBLE);

            if (!lastMessage.isIncoming()) {
                viewHolder.outgoingMessageIndicator.setText(context.getString(R.string.sender_is_you) + ": ");
                viewHolder.outgoingMessageIndicator.setVisibility(View.VISIBLE);
                viewHolder.outgoingMessageIndicator.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(contact.getAccount()));
            }
            viewHolder.smallRightIcon.setImageResource(R.drawable.ic_client_small);
            viewHolder.smallRightIcon.setVisibility(View.VISIBLE);
            viewHolder.smallRightIcon.setImageLevel(clientSoftware.ordinal());
            viewHolder.largeClientIcon.setVisibility(View.GONE);
        }

        if (messageManager.hasActiveChat(contact.getAccount(), contact.getUser())) {

            view.setBackgroundColor(ColorManager.getInstance().getActiveChatBackgroundColor());
            viewHolder.separator.setBackgroundColor(ColorManager.getInstance().getActiveChatSeparatorColor());
            viewHolder.largeClientIcon.setColorFilter(ColorManager.getInstance().getActiveChatLargeClientIconColor());
            viewHolder.smallRightIcon.setColorFilter(ColorManager.getInstance().getActiveChatLargeClientIconColor());
            viewHolder.smallRightText.setTextColor(ColorManager.getInstance().getActiveChatLargeClientIconColor());
        } else {
            view.setBackgroundColor(ColorManager.getInstance().getContactBackground());
            viewHolder.separator.setBackgroundColor(ColorManager.getInstance().getContactSeparatorColor());
            viewHolder.largeClientIcon.setColorFilter(ColorManager.getInstance().getContactLargeClientIconColor());
            viewHolder.smallRightIcon.setColorFilter(ColorManager.getInstance().getContactLargeClientIconColor());
            viewHolder.smallRightText.setTextColor(ColorManager.getInstance().getContactLargeClientIconColor());
        }

        if (statusText.isEmpty()) {
            viewHolder.secondLineMessage.setVisibility(View.GONE);
        } else {
            viewHolder.secondLineMessage.setVisibility(View.VISIBLE);
            viewHolder.secondLineMessage.setText(Emoticons.getSmiledText(context, statusText, viewHolder.secondLineMessage));
        }

        viewHolder.statusIcon.setImageLevel(contact.getStatusMode().getStatusLevel());
        return view;
    }


    private void onAvatarClick(AbstractContact contact) {
        Intent intent;
        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            intent = ContactViewer.createIntent(context, contact.getAccount(), contact.getUser());
        } else {
            intent = ContactEditor.createIntent(context, contact.getAccount(), contact.getUser());
        }
        context.startActivity(intent);
    }
}

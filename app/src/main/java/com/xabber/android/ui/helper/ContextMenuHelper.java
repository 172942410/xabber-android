/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.helper;

import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.android.ui.ContactAdd;
import com.xabber.android.ui.GroupEditor;
import com.xabber.android.ui.MUCEditor;
import com.xabber.android.ui.StatusEditor;
import com.xabber.android.ui.adapter.UpdatableAdapter;
import com.xabber.android.ui.dialog.ContactDeleteDialogFragment;
import com.xabber.android.ui.dialog.GroupDeleteDialogFragment;
import com.xabber.android.ui.dialog.GroupRenameDialogFragment;
import com.xabber.android.ui.dialog.MUCDeleteDialogFragment;
import com.xabber.android.ui.preferences.AccountEditor;
import com.xabber.android.ui.ContactViewer;
import com.xabber.androiddev.R;

/**
 * Helper class for context menu creation.
 *
 * @author alexander.ivanov
 */
public class ContextMenuHelper {

    private ContextMenuHelper() {
    }

    public static void createContactContextMenu(
            final FragmentActivity activity, final UpdatableAdapter adapter,
            AbstractContact abstractContact, ContextMenu menu) {
        final String account = abstractContact.getAccount();
        final String user = abstractContact.getUser();
        menu.setHeaderTitle(abstractContact.getName());
        if (MUCManager.getInstance().hasRoom(account, user)) {
            if (!MUCManager.getInstance().inUse(account, user))
                menu.add(R.string.muc_edit).setIntent(
                        MUCEditor.createIntent(activity, account, user));
            menu.add(R.string.muc_delete).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            MUCDeleteDialogFragment.newInstance(account, user)
                                    .show(activity.getFragmentManager(),
                                            "MUC_DELETE");
                            return true;
                        }

                    });
            if (MUCManager.getInstance().isDisabled(account, user))
                menu.add(R.string.muc_join).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                MUCManager.getInstance().joinRoom(account,
                                        user, true);
                                return true;
                            }

                        });
            else
                menu.add(R.string.muc_leave).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                MUCManager.getInstance().leaveRoom(account,
                                        user);
                                MessageManager.getInstance().closeChat(account,
                                        user);
                                NotificationManager.getInstance()
                                        .removeMessageNotification(account,
                                                user);
                                adapter.onChange();
                                return true;
                            }

                        });
        } else {
            menu.add(R.string.contact_delete).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            ContactDeleteDialogFragment.newInstance(account,
                                    user).show(activity.getFragmentManager(),
                                    "CONTACT_DELETE");
                            return true;
                        }

                    });
            if (MessageManager.getInstance().hasActiveChat(account, user))
                menu.add(R.string.close_chat).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                MessageManager.getInstance().closeChat(account,
                                        user);
                                NotificationManager.getInstance()
                                        .removeMessageNotification(account,
                                                user);
                                adapter.onChange();
                                return true;
                            }

                        });
            if (abstractContact.getStatusMode() == StatusMode.unsubscribed)
                menu.add(R.string.request_subscription)
                        .setOnMenuItemClickListener(
                                new MenuItem.OnMenuItemClickListener() {

                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        try {
                                            PresenceManager.getInstance()
                                                    .requestSubscription(
                                                            account, user);
                                        } catch (NetworkException e) {
                                            Application.getInstance()
                                                    .onError(e);
                                        }
                                        return true;
                                    }

                                });
        }
        if (PresenceManager.getInstance().hasSubscriptionRequest(account, user)) {
            menu.add(R.string.accept_subscription).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                PresenceManager.getInstance()
                                        .acceptSubscription(account, user);
                            } catch (NetworkException e) {
                                Application.getInstance().onError(e);
                            }
                            activity.startActivity(GroupEditor.createIntent(
                                    activity, account, user));
                            return true;
                        }

                    });
            menu.add(R.string.discard_subscription).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                PresenceManager.getInstance()
                                        .discardSubscription(account, user);
                            } catch (NetworkException e) {
                                Application.getInstance().onError(e);
                            }
                            return true;
                        }

                    });
        }
    }

    public static void createGroupContextMenu(final FragmentActivity activity,
                                              UpdatableAdapter adapter, final String account, final String group,
                                              ContextMenu menu) {
        menu.setHeaderTitle(GroupManager.getInstance().getGroupName(account,
                group));
        if (group != GroupManager.ACTIVE_CHATS && group != GroupManager.IS_ROOM) {
            menu.add(R.string.group_rename).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            GroupRenameDialogFragment.newInstance(
                                    account == GroupManager.NO_ACCOUNT ? null
                                            : account,
                                    group == GroupManager.NO_GROUP ? null
                                            : group).show(activity.getFragmentManager(),
                                    "GROUP_RENAME");
                            return true;
                        }
                    });
            if (group != GroupManager.NO_GROUP)
                menu.add(R.string.group_remove).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                GroupDeleteDialogFragment
                                        .newInstance(
                                                account == GroupManager.NO_ACCOUNT ? null
                                                        : account, group)
                                        .show(activity.getFragmentManager(), "GROUP_DELETE");
                                return true;
                            }
                        });
        }
        createOfflineModeContextMenu(adapter, account, group, menu);
    }

    public static void createAccountContextMenu(
            final FragmentActivity activity, UpdatableAdapter adapter,
            final String account, ContextMenu menu) {
        menu.setHeaderTitle(AccountManager.getInstance()
                .getVerboseName(account));
        AccountItem accountItem = AccountManager.getInstance().getAccount(
                account);
        ConnectionState state = accountItem.getState();
        if (state == ConnectionState.waiting)
            menu.add(R.string.account_reconnect).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (AccountManager.getInstance()
                                    .getAccount(account).updateConnection(true))
                                AccountManager.getInstance().onAccountChanged(
                                        account);
                            return true;
                        }

                    });
        menu.add(R.string.status_editor).setIntent(
                StatusEditor.createIntent(activity, account));
        menu.add(R.string.account_editor).setIntent(
                AccountEditor.createIntent(activity, account));
        if (state.isConnected()) {
            menu.add(R.string.contact_viewer).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            String user = AccountManager.getInstance()
                                    .getAccount(account).getRealJid();
                            if (user == null)
                                Application.getInstance().onError(
                                        R.string.NOT_CONNECTED);
                            else {
                                activity.startActivity(ContactViewer
                                        .createIntent(activity, account, user));
                            }
                            return true;
                        }
                    });
            menu.add(R.string.contact_add).setIntent(
                    ContactAdd.createIntent(activity, account));
        }
        if (SettingsManager.contactsShowAccounts())
            createOfflineModeContextMenu(adapter, account, null, menu);
    }

    private static void createOfflineModeContextMenu(UpdatableAdapter adapter,
                                                     String account, String group, ContextMenu menu) {
        SubMenu mapMode = menu.addSubMenu(R.string.show_offline_settings);
        mapMode.setHeaderTitle(R.string.show_offline_settings);
        MenuItem always = mapMode.add(R.string.show_offline_settings, 0, 0,
                R.string.show_offline_always).setOnMenuItemClickListener(
                new OfflineModeClickListener(adapter, account, group,
                        ShowOfflineMode.always));
        MenuItem normal = mapMode.add(R.string.show_offline_settings, 0, 0,
                R.string.show_offline_normal).setOnMenuItemClickListener(
                new OfflineModeClickListener(adapter, account, group,
                        ShowOfflineMode.normal));
        MenuItem never = mapMode.add(R.string.show_offline_settings, 0, 0,
                R.string.show_offline_never).setOnMenuItemClickListener(
                new OfflineModeClickListener(adapter, account, group,
                        ShowOfflineMode.never));
        mapMode.setGroupCheckable(R.string.show_offline_settings, true, true);
        ShowOfflineMode showOfflineMode = GroupManager.getInstance()
                .getShowOfflineMode(account,
                        group == null ? GroupManager.IS_ACCOUNT : group);
        if (showOfflineMode == ShowOfflineMode.always)
            always.setChecked(true);
        else if (showOfflineMode == ShowOfflineMode.normal)
            normal.setChecked(true);
        else if (showOfflineMode == ShowOfflineMode.never)
            never.setChecked(true);
        else
            throw new IllegalStateException();
    }

    private static class OfflineModeClickListener implements
            MenuItem.OnMenuItemClickListener {

        private final UpdatableAdapter adapter;
        private final String account;
        private final String group;
        private final ShowOfflineMode mode;

        public OfflineModeClickListener(UpdatableAdapter adapter,
                                        String account, String group, ShowOfflineMode mode) {
            super();
            this.adapter = adapter;
            this.account = account;
            this.group = group;
            this.mode = mode;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            GroupManager.getInstance().setShowOfflineMode(account,
                    group == null ? GroupManager.IS_ACCOUNT : group, mode);
            adapter.onChange();
            return true;
        }

    }

}

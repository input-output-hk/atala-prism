package io.iohk.cvp.views.utils.adapters;

import io.iohk.cvp.data.local.db.model.Credential;

public interface CredentialClickListener {
    void onCredentialClickListener(Boolean isNew, Credential credential);
}

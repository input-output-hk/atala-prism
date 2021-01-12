package io.iohk.atala.prism.app.views.utils.adapters;

import io.iohk.atala.prism.app.data.local.db.model.Credential;

public interface CredentialClickListener {
    void onCredentialClickListener(Boolean isNew, Credential credential);
}

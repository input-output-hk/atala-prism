package io.iohk.cvp.views.interfaces;

import io.iohk.cvp.viewmodel.dtos.ConnectionListable;

public interface SelectedVerifiersUpdateable {
    void updateSelectedVerifiers(ConnectionListable connection, Boolean isSelected);
    void updateButtonState();
}

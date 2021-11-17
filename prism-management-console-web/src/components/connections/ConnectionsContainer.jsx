import React from 'react';
import { observer } from 'mobx-react-lite';
import Connections from './Connections';
import { useContactStore, useContactUiState } from '../../hooks/useContactStore';

const ConnectionsContainer = observer(() => {
  const { contactUiState } = useContactStore();
  useContactUiState({ instance: contactUiState, reset: true });

  return <Connections />;
});

export default ConnectionsContainer;

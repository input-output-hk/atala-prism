import React from 'react';
import { observer } from 'mobx-react-lite';
import Connections from './Connections';
import { useContactStore } from '../../hooks/useContactStore';

const ConnectionsContainer = observer(() => {
  const { resetUiState } = useContactStore();
  resetUiState();

  return <Connections />;
});

export default ConnectionsContainer;

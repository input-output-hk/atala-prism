import React from 'react';
import { useHistory } from 'react-router-dom';

const MainController = async ({ isWalletUnlocked }) => {
  if (!isWalletUnlocked) {
    const history = useHistory();
    history.push('/login');
  }
};

export default MainController;

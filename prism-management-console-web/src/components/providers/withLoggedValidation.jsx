import React from 'react';
import { observer } from 'mobx-react-lite';
import Landing from '../landing/Landing';
import Loading from '../common/Atoms/Loading/Loading';
import { withApi } from './withApi';
import { useSession } from '../../hooks/useSession';
import { LOADING, UNLOCKED } from '../../helpers/constants';

const withLoggedValidationComponent = Component =>
  observer(props => {
    const { session } = useSession();
    debugger;
    if (session?.sessionState === LOADING) return <Loading />;

    // Here is where the validations are made
    const walletSessionIsValid = session?.sessionState === UNLOCKED;

    // If the logged in user meets the requeriments,
    // the wanted component is returned.
    if (walletSessionIsValid) return <Component {...props} />;

    // Else, the landing is returned.
    return <Landing />;
  });

export const withLoggedValidation = Component => withApi(withLoggedValidationComponent(Component));

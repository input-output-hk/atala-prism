import React, { useState, useRef, useEffect, useContext } from 'react';
import { Col, Row } from 'antd';
import PropTypes from 'prop-types';
import UserInfo from './UserInfo';
import PersonalInformation from './Organisms/PersonalInformation/PersonalInformation';
import IdentityVerifierModal from './Molecules/IdentityVerifierModal/IdentityVerifierModal';
import { UserContext } from '../providers/userContext';
import { withRedirector } from '../providers/withRedirector';
import CredentialsView from './Molecules/CredentialsView/CredentialsView';

import './_style.scss';

const UserInfoContainer = ({ redirector: { redirectToCredentials } }) => {
  const { user, setUser } = useContext(UserContext);
  const [showModal, setShowModal] = useState();

  const personalInfoRef = useRef();

  useEffect(() => {
    if (user.firstName) {
      setShowModal(true);
    }
  }, [user.firstName]);

  const acceptIdentity = () => {
    redirectToCredentials();
  };

  const submitForm = () => {
    personalInfoRef.current
      .getForm()
      .validateFieldsAndScroll(
        ['dateOfBirth', 'firstName'],
        (errors, { dateOfBirth, firstName }) => {
          if (errors) return;

          setUser({
            ...user,
            firstName,
            dateOfBirth
          });
          redirectToCredentials();
        }
      );
  };

  const getStep = () => (
    <div className="FirstStep">
      <Row>
        <Col xs={24} lg={6}>
          <CredentialsView />
        </Col>
        <Col xs={24} lg={18}>
          <PersonalInformation nextStep={submitForm} personalInfoRef={personalInfoRef} />
        </Col>
      </Row>
    </div>
  );

  const cleanUser = () => {
    setUser(null);
    setShowModal(false);
  };

  return (
    <div>
      <IdentityVerifierModal
        showModal={showModal}
        onOk={acceptIdentity}
        onCancel={cleanUser}
        user={user}
      />
      <UserInfo getStep={getStep} />;
    </div>
  );
};

UserInfoContainer.propTypes = {
  redirector: PropTypes.shape({
    redirectToCredentials: PropTypes.func
  }).isRequired
};

export default withRedirector(UserInfoContainer);

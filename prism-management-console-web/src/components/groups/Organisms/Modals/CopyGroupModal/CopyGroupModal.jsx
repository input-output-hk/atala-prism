import React, { createRef, useEffect, useState } from 'react';
import { Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { groupShape } from '../../../../../helpers/propShapes';
import GroupForm from '../../../../common/Molecules/GroupForm/GroupFormContainer';
import { GROUP_NAME_STATES } from '../../../../../helpers/constants';
import copyIcon from '../../../../../images/copyIcon.svg';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const CopyGroupModal = ({ open, closeModal, group, onSave }) => {
  const { t } = useTranslation();
  const formRef = createRef();
  const [copyName, setCopyName] = useState('');
  const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);

  useEffect(() => {
    setCopyName(t('groups.copy.copyOf', { name: group?.name }));
    setNameState(GROUP_NAME_STATES.initial);
  }, [group, t]);

  return (
    <Modal destroyOnClose visible={open} footer={null} onCancel={closeModal}>
      <div className="CopyGroupModal">
        <img src={copyIcon} alt="copyIcon" />
        <h1>{t('groups.copy.title')}</h1>
        <h3>{t('groups.copy.subtitle')}</h3>
        <div className="NewNameContainer">
          <p>{t('groups.copy.label')}</p>
          <GroupForm
            className="NewName"
            formRef={formRef}
            updateForm={setCopyName}
            formValues={{ groupName: copyName }}
            nameState={nameState}
            setNameState={setNameState}
          />
        </div>
        <div className="ButtonsContainer">
          <CustomButton
            buttonText={t('actions.cancel')}
            buttonProps={{ className: 'theme-secondary', onClick: closeModal }}
          />
          <CustomButton
            buttonText={t('actions.save')}
            buttonProps={{
              className: 'theme-primary',
              onClick: () => onSave(copyName),
              disabled: nameState !== GROUP_NAME_STATES.possible
            }}
          />
        </div>
      </div>
    </Modal>
  );
};

CopyGroupModal.defaultProps = {
  group: {}
};

CopyGroupModal.propTypes = {
  group: PropTypes.shape(groupShape),
  closeModal: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
  onSave: PropTypes.func.isRequired
};

export default CopyGroupModal;

import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import IconPickerModal from '../IconPickerModal/IconPickerModal';
import { useTemplateCreationStore } from '../../../../hooks/useTemplatesPageStore';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import './_style.scss';

const i18nPrefix = 'credentialTemplateCreation.templateIcon';

const TemplateIconSection = observer(() => {
  const { t } = useTranslation();
  const { templateSketch } = useTemplateCreationStore();
  const [showIconPickerModal, setShowIconPickerModal] = useState(false);

  return (
    <div className="TemplateIconContainer flex">
      <div className="TemplateIconWrapper">
        <img className="TemplateIcon" src={templateSketch.icon} alt="template icon" />
      </div>
      <div className="verticalFlex">
        <p className="TitleSmall">{t(`${i18nPrefix}.title`)}</p>
        <p className="SubtitleGray">{t(`${i18nPrefix}.info`)}</p>
        <CustomButton
          buttonProps={{
            onClick: () => setShowIconPickerModal(true),
            className: 'theme-outline'
          }}
          buttonText={t(`${i18nPrefix}.changeIcon`)}
        />
      </div>

      <IconPickerModal visible={showIconPickerModal} close={() => setShowIconPickerModal(false)} />
    </div>
  );
});

export default TemplateIconSection;

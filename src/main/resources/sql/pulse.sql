/**
 ID : getQuestionnaireTemplateById
 */
select
       qt.id,
       qt.status,
       qt.target_type as targetType,
       qt.partner_id as partnerId,
       qt.master_emp_id as employeeId,
       qrt.id as richId,
       qrt.rich_title as richTitle,
       qrt.rich_desc as richDesc,
       qp.id as profileId,
       qp.is_show_order as isShowOrder,
       qp.is_random_order as isRandomOrder,
       qp.welcome_pic as welcomePic,
       qp.welcome_pic_mobile as welcomePicMobile,
       qp.is_limit as isLimit,
       qp.is_repeat as isRepeat,
       qp.is_expired as isExpired,
       qp.expired_date as expiredDate,
       (
       select
              group_concat(emp_id)
       from
            questionnaire_authorized qa
       where
           qa.qnr_id = qt.id
       ) as authorizedEmployees
FROM
     questionnaire_template qt
       LEFT JOIN questionnaire_profile qp on qp.qnr_id = qt.id
       LEFT JOIN questionnaire_rich_text qrt on qrt.qnr_id = qt.id
where
    qt.id = ?;